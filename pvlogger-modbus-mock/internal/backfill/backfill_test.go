package backfill

import (
	"encoding/json"
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
		f.raw[*row.DateCreated] = row
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

	if err := Run(p, client, from, until); err != nil {
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

func TestADayIsFilledOnlyBeforeItsFirstRow(t *testing.T) {
	loc := zone(t)
	p := plant.New(plant.Config{Location: loc})
	da, client := newFakeDataAccessor(t, loc)
	from := time.Date(2025, 6, 20, 22, 0, 0, 0, loc)
	until := time.Date(2025, 6, 21, 2, 0, 0, 0, loc)
	// Live data since 01:00 on the 21st.
	live := time.Date(2025, 6, 21, 1, 0, 0, 0, loc)
	da.kpis[live.UnixMilli()] = kpiRow{DateCreated: live.UnixMilli()}

	if err := Run(p, client, from, until); err != nil {
		t.Fatal(err)
	}
	// 22:00–00:00 on the 20th and 00:00–01:00 on the 21st.
	if len(da.raw) != 180 {
		t.Errorf("stored %d raw rows, want 180", len(da.raw))
	}
	for ms := range da.raw {
		if !time.UnixMilli(ms).Before(live) {
			t.Errorf("backfilled %s, after the live data began", time.UnixMilli(ms).In(loc).Format(time.DateTime))
		}
	}

	stored := len(da.raw)
	if err := Run(p, client, from, until); err != nil {
		t.Fatal(err)
	}
	if len(da.raw) != stored {
		t.Errorf("a second run stored %d more raw rows", len(da.raw)-stored)
	}
}

func near(t *testing.T, at time.Time, what string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 1e-6 {
		t.Errorf("%s: %s = %v, want %v", at.Format(time.DateTime), what, got, want)
	}
}
