package backfill

import (
	"context"
	"encoding/json"
	"errors"
	"math"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"pvlogger-shared-modbus/persistence"

	"pvlogger-modbus-mock/internal/mock"
	"pvlogger-modbus-mock/internal/plant"
)

// rawRow and kpiRow are the JSON the data accessor receives.
type rawRow struct {
	JobId                    string  `json:"jobId"`
	InverterActivePower      float64 `json:"inverterActivePower"`
	MeterActivePower         float64 `json:"meterActivePower"`
	InverterDailyYield       float64 `json:"inverterDailyYield"`
	InverterAccumulatedYield float64 `json:"inverterAccumulatedYield"`
	MeterExportedEnergy      float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy      float64 `json:"meterImportedEnergy"`
	DateCreated              *int64  `json:"dateCreated"`
}

type kpiRow struct {
	DateCreated   int64              `json:"dateCreated"`
	Producing     map[string]float64 `json:"producing"`
	Exporting     map[string]float64 `json:"exporting"`
	Importing     map[string]float64 `json:"importing"`
	Consuming     map[string]float64 `json:"consuming"`
	SelfConsuming map[string]float64 `json:"selfConsuming"`
	Produced      map[string]float64 `json:"produced"`
	Exported      map[string]float64 `json:"exported"`
	Imported      map[string]float64 `json:"imported"`
	Consumed      map[string]float64 `json:"consumed"`
	SelfConsumed  map[string]float64 `json:"selfConsumed"`
}

// fakeDataAccessor holds what was stored and answers /v1/daydata/{date} in loc,
// as the data accessor does in its own zone.
type fakeDataAccessor struct {
	loc  *time.Location
	mu   sync.Mutex
	raw  map[int64]rawRow
	kpis map[int64]kpiRow
	// order holds the raw rows' dateCreated in the order they arrived;
	// failAfter > 0 refuses every raw row after that many.
	order     []int64
	failAfter int
}

func newFakeDataAccessor(t *testing.T, loc *time.Location) (*fakeDataAccessor, *persistence.PersistingService) {
	f := &fakeDataAccessor{loc: loc, raw: map[int64]rawRow{}, kpis: map[int64]kpiRow{}}
	srv := httptest.NewServer(f)
	t.Cleanup(srv.Close)
	u, _ := url.Parse(srv.URL)
	port, _ := strconv.Atoi(u.Port())
	return f, persistence.NewPersistingService(u.Hostname(), port, "http", "v1")
}

func (f *fakeDataAccessor) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	f.mu.Lock()
	defer f.mu.Unlock()
	switch {
	case r.Method == http.MethodPost && r.URL.Path == "/v1/store-raw-kpi-register-values":
		var row rawRow
		if json.NewDecoder(r.Body).Decode(&row) != nil || row.DateCreated == nil {
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		if f.failAfter > 0 && len(f.order) >= f.failAfter {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		f.raw[*row.DateCreated] = row
		f.order = append(f.order, *row.DateCreated)
	case r.Method == http.MethodPost && r.URL.Path == "/v1/store-kpi-values":
		var row kpiRow
		if json.NewDecoder(r.Body).Decode(&row) != nil {
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		f.kpis[row.DateCreated] = row
	case r.Method == http.MethodGet && strings.HasPrefix(r.URL.Path, "/v1/daydata/"):
		day, err := time.ParseInLocation("2006-1-2", strings.TrimPrefix(r.URL.Path, "/v1/daydata/"), f.loc)
		if err != nil {
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		rows := []map[string]int64{}
		for ms := range f.kpis {
			if at := time.UnixMilli(ms).In(f.loc); !at.Before(day) && at.Before(day.AddDate(0, 0, 1)) {
				rows = append(rows, map[string]int64{"createdAt": ms})
			}
		}
		_ = json.NewEncoder(w).Encode(rows)
	default:
		w.WriteHeader(http.StatusNotFound)
	}
}

func zone(t *testing.T) *time.Location {
	loc, err := time.LoadLocation("Europe/Berlin")
	if err != nil {
		t.Fatal(err)
	}
	return loc
}

func TestEveryMinuteGetsTheReadingAndTheDaysEnergy(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 20, 23, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 13, 0, 0, 0, loc)

	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}

	minutes := int(until.Sub(from).Minutes())
	if len(da.raw) != minutes || len(da.kpis) != minutes {
		t.Fatalf("stored %d raw and %d KPI rows, want %d each", len(da.raw), len(da.kpis), minutes)
	}
	for at := from; at.Before(until); at = at.Add(time.Minute) {
		ms := at.UnixMilli()
		raw, kpi := da.raw[ms], da.kpis[ms]
		r := mock.ReadingOf(p.At(at))
		midnight := mock.ReadingOf(p.At(time.Date(at.Year(), at.Month(), at.Day(), 0, 0, 0, 0, loc)))

		// The raw row is what the fast collector stores for this reading.
		near(t, at, "raw inverter active power kW", raw.InverterActivePower, r.PVW/1000)
		near(t, at, "raw meter active power W", raw.MeterActivePower, -r.GridImportW)
		near(t, at, "raw daily yield", raw.InverterDailyYield, r.YieldTodayKWh)
		near(t, at, "raw accumulated yield", raw.InverterAccumulatedYield, r.YieldTotalKWh)
		near(t, at, "raw exported", raw.MeterExportedEnergy, r.ExportedTotalKWh)
		near(t, at, "raw imported", raw.MeterImportedEnergy, r.ImportedTotalKWh)

		// The KPI row: live power from the reading, energy since local midnight.
		near(t, at, "producing", kpi.Producing["w"], r.PVW)
		near(t, at, "consuming", kpi.Consuming["w"], r.LoadW)
		near(t, at, "exporting", kpi.Exporting["w"], math.Max(-r.GridImportW, 0))
		near(t, at, "importing", kpi.Importing["w"], math.Max(r.GridImportW, 0))
		near(t, at, "produced Wh", kpi.Produced["wh"], 1000*(r.YieldTotalKWh-midnight.YieldTotalKWh))
		near(t, at, "exported Wh", kpi.Exported["wh"], 1000*(r.ExportedTotalKWh-midnight.ExportedTotalKWh))
		near(t, at, "imported Wh", kpi.Imported["wh"], 1000*(r.ImportedTotalKWh-midnight.ImportedTotalKWh))
		near(t, at, "consumed Wh", kpi.Consumed["wh"], kpi.Produced["wh"]-kpi.Exported["wh"]+kpi.Imported["wh"])
		if t.Failed() {
			return
		}
	}
}

func TestOnlyMinutesWithoutARowAreFilled(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 20, 22, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 2, 0, 0, 0, loc)
	// Live data from 01:00 to 01:30 on the 21st, a few seconds into each minute.
	live := time.Date(2025, 6, 21, 1, 0, 0, 0, loc)
	for at := live; at.Before(live.Add(30 * time.Minute)); at = at.Add(time.Minute) {
		ms := at.Add(7 * time.Second).UnixMilli()
		da.kpis[ms] = kpiRow{DateCreated: ms}
	}

	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	// 4 hours minus the 30 live minutes.
	if len(da.raw) != 210 {
		t.Errorf("stored %d raw rows, want 210", len(da.raw))
	}
	for ms := range da.raw {
		if at := time.UnixMilli(ms); !at.Before(live) && at.Before(live.Add(30*time.Minute)) {
			t.Errorf("backfilled %s, a minute with live data", at.In(loc).Format(time.DateTime))
		}
	}

	stored := len(da.raw)
	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	if len(da.raw) != stored {
		t.Errorf("a second run stored %d more raw rows", len(da.raw)-stored)
	}
}

func TestARerunCompletesAnInterruptedRun(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 21, 0, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 6, 0, 0, 0, loc)

	da.failAfter = 100
	if err := Run(context.Background(), p, client, from, until); err == nil {
		t.Fatal("the interrupted run reported no error")
	}
	da.failAfter = 0
	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	if len(da.raw) != 360 || len(da.kpis) != 360 {
		t.Errorf("stored %d raw and %d KPI rows, want 360 each", len(da.raw), len(da.kpis))
	}
}

func TestYesterdayIsFilledFirst(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 18, 23, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 0, 30, 0, 0, loc)

	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	var days []string
	for _, ms := range da.order {
		if d := time.UnixMilli(ms).In(loc).Format(time.DateOnly); len(days) == 0 || days[len(days)-1] != d {
			days = append(days, d)
		}
	}
	want := []string{"2025-06-20", "2025-06-21", "2025-06-19", "2025-06-18"}
	if strings.Join(days, " ") != strings.Join(want, " ") {
		t.Errorf("filled the days in the order %v, want %v", days, want)
	}
}

// America/Santiago starts DST at midnight: 2025-09-07 begins at 01:00.
func TestDaysStayAlignedWhereMidnightIsSkipped(t *testing.T) {
	loc, err := time.LoadLocation("America/Santiago")
	if err != nil {
		t.Fatal(err)
	}
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 9, 6, 22, 0, 0, 0, loc)
	until := time.Date(2025, 9, 8, 2, 0, 0, 0, loc)

	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	if minutes := int(until.Sub(from).Minutes()); len(da.raw) != minutes {
		t.Errorf("stored %d raw rows, want %d", len(da.raw), minutes)
	}
	for ms := range da.raw {
		if at := time.UnixMilli(ms).In(loc); at.Before(from) || !at.Before(until) {
			t.Errorf("backfilled %s, outside [from, until)", at.Format(time.DateTime))
		}
	}
	if got := plant.StartOfDay(time.Date(2025, 9, 7, 12, 0, 0, 0, loc)); got.Hour() != 1 || got.Day() != 7 {
		t.Errorf("the 7th starts at %s, want 01:00 on the 7th", got.Format(time.DateTime))
	}
}

func near(t *testing.T, at time.Time, what string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 1e-6 {
		t.Errorf("%s: %s = %v, want %v", at.Format(time.DateTime), what, got, want)
	}
}

// SIGTERM ends a run between two minutes; a rerun completes the history.
func TestACancelledRunStopsAndARerunCompletesIt(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 21, 0, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 6, 0, 0, 0, loc)

	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	if err := Run(ctx, p, client, from, until); !errors.Is(err, context.Canceled) {
		t.Fatalf("got %v, want context.Canceled", err)
	}
	if len(da.raw) != 0 {
		t.Fatalf("a cancelled run stored %d raw rows", len(da.raw))
	}
	if err := Run(context.Background(), p, client, from, until); err != nil {
		t.Fatal(err)
	}
	if len(da.raw) != 360 || len(da.kpis) != 360 {
		t.Errorf("stored %d raw and %d KPI rows, want 360 each", len(da.raw), len(da.kpis))
	}
}
