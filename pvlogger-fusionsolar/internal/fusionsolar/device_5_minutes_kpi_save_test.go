package fusionsolar

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"testing"
	"time"
	_ "time/tzdata" // the tests set a site zone; not every CI image ships zoneinfo

	"pvlogger-fusionsolar/internal/database"
	"pvlogger-fusionsolar/internal/model"
	"pvlogger-fusionsolar/internal/repository"
)

// fakeRepo keeps the inserted device KPI rows in memory. Its
// SelectFormer5MinDeviceRealKpi does in Go what the SQL query does (type
// DEVICE_5_MIN_KPI, collect_time not null, distinct per device and time,
// counted per day in the process's zone); the repository's database test
// checks the query against the same rules.
type fakeRepo struct {
	nextID     int32
	deviceRows []repository.DeviceRealKpiInput
}

func (r *fakeRepo) InsertFsResponse(context.Context, repository.FsResponseInput) (int32, error) {
	r.nextID++
	return r.nextID, nil
}
func (r *fakeRepo) InsertFsResponseParamsExtended(context.Context, repository.FsResponseParamsExtendedInput) error {
	return nil
}
func (r *fakeRepo) InsertFsResponseParamsBasic(context.Context, repository.FsResponseParamsBasicInput) error {
	return nil
}
func (r *fakeRepo) InsertFsResponseParamsDevice(context.Context, repository.FsResponseParamsDeviceInput) error {
	return nil
}
func (r *fakeRepo) UpsertStationExtendedData(context.Context, repository.StationExtendedDataInput) error {
	return nil
}
func (r *fakeRepo) InsertStationExtendedDataMapping(context.Context, repository.StationExtendedDataMappingInput) error {
	return nil
}
func (r *fakeRepo) InsertStationRealKpi(context.Context, repository.StationRealKpiInput) error {
	return nil
}
func (r *fakeRepo) InsertDeviceRealKpi(_ context.Context, input repository.DeviceRealKpiInput) error {
	r.deviceRows = append(r.deviceRows, input)
	return nil
}
func (r *fakeRepo) SelectFormer5MinDeviceRealKpi(context.Context) (map[time.Time]int, error) {
	seen := map[string]bool{}
	var collectTimes []int64
	for _, row := range r.deviceRows {
		if row.Type != "DEVICE_5_MIN_KPI" || !row.CollectTime.Valid {
			continue
		}
		key := fmt.Sprintf("%d/%d", row.DevId.Int64, row.CollectTime.Int64)
		if seen[key] {
			continue
		}
		seen[key] = true
		collectTimes = append(collectTimes, row.CollectTime.Int64)
	}
	counts := map[time.Time]int{}
	for _, ms := range collectTimes {
		t := time.UnixMilli(ms)
		counts[time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, time.UTC)]++
	}
	return counts, nil
}

// withSiteZone runs the test in a zone east of UTC, as the chart's TZ does in
// production: the site's day starts before midnight UTC.
func withSiteZone(t *testing.T) {
	t.Helper()
	zone, err := time.LoadLocation("Europe/Berlin")
	if err != nil {
		t.Fatal(err)
	}
	old := time.Local
	time.Local = zone
	t.Cleanup(func() { time.Local = old })
}

// fiveMinutesResponse is FusionSolar's reply for one site day: a point every
// 5 minutes per device, from local midnight on.
func fiveMinutesResponse(t *testing.T, day time.Time, devIds ...int64) *model.DeviceFiveMinutesKpiResponse {
	t.Helper()
	start := time.Date(day.Year(), day.Month(), day.Day(), 0, 0, 0, 0, time.Local)
	var data []string
	for _, dev := range devIds {
		for ts := start; ts.Day() == start.Day(); ts = ts.Add(5 * time.Minute) {
			data = append(data, fmt.Sprintf(`{"devId":%d,"collectTime":%d,"dataItemMap":{"active_power":1.5}}`, dev, ts.UnixMilli()))
		}
	}
	body := fmt.Sprintf(`{"success":true,"failCode":0,"params":{"devIds":"1,2","currentTime":%d,"collectTime":%d},"data":[%s]}`,
		day.UnixMilli(), day.UnixMilli(), strings.Join(data, ","))
	response := &model.DeviceFiveMinutesKpiResponse{Body: []byte(body)}
	if err := json.Unmarshal(response.Body, &response.JSON200); err != nil {
		t.Fatal(err)
	}
	return response
}

func newTestAccess(repo *fakeRepo) *FusionSolarAccess {
	return &FusionSolarAccess{repo: repo, db: &database.Database{Ctx: context.Background()}}
}

// completeUntil marks every day from the walker's start up to and including
// last as collected.
func completeUntil(last time.Time) map[time.Time]int {
	records := map[time.Time]int{}
	for d := time.Date(2022, 10, 25, 0, 0, 0, 0, time.UTC); !d.After(last); d = d.Add(24 * time.Hour) {
		records[d] = minRowsPerDay
	}
	return records
}

func merge(a, b map[time.Time]int) map[time.Time]int {
	out := map[time.Time]int{}
	for k, v := range a {
		out[k] += v
	}
	for k, v := range b {
		out[k] += v
	}
	return out
}

// Backlog 66: the job stored its rows as DEVICE_REAL_KPI without collect_time,
// so the gap search never saw them and refetched the same day on every run.
func TestSaveDev5MinKPIs_SavedDayCountsAndNextDateMoves(t *testing.T) {
	withSiteZone(t)
	day := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)
	now := time.Date(2026, 11, 19, 12, 0, 0, 0, time.UTC)
	history := completeUntil(day.Add(-24 * time.Hour))
	if next := determineNextCollectionDate(history, now); next == nil || !next.Equal(day) {
		t.Fatalf("before saving: next date %v, want %v", next, day)
	}

	repo := &fakeRepo{}
	access := newTestAccess(repo)
	if err := access.SaveDev5MinKPIs(fiveMinutesResponse(t, day, 1, 2)); err != nil {
		t.Fatal(err)
	}

	for i, row := range repo.deviceRows {
		if row.Type != "DEVICE_5_MIN_KPI" || !row.CollectTime.Valid {
			t.Fatalf("row %d stored as type %q, collect_time valid %v", i, row.Type, row.CollectTime.Valid)
		}
	}
	saved, err := repo.SelectFormer5MinDeviceRealKpi(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	if saved[day] < minRowsPerDay {
		t.Fatalf("saved day counts %d rows, want at least %d", saved[day], minRowsPerDay)
	}
	want := day.Add(24 * time.Hour)
	if next := determineNextCollectionDate(merge(history, saved), now); next == nil || !next.Equal(want) {
		t.Fatalf("after saving: next date %v, want %v", next, want)
	}
	if err := checkCollectionProgress(day, 0, saved[day]); err != nil {
		t.Fatalf("progress check: %v", err)
	}
}

// A refetch of a day already stored adds no distinct rows: the run must fail,
// so a stuck job shows up as a failed Job instead of a quiet success.
func TestSaveDev5MinKPIs_RefetchIsReportedAsStuck(t *testing.T) {
	withSiteZone(t)
	day := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)
	repo := &fakeRepo{}
	access := newTestAccess(repo)
	if err := access.SaveDev5MinKPIs(fiveMinutesResponse(t, day, 1, 2)); err != nil {
		t.Fatal(err)
	}
	before, _ := repo.SelectFormer5MinDeviceRealKpi(context.Background())
	if err := access.SaveDev5MinKPIs(fiveMinutesResponse(t, day, 1, 2)); err != nil {
		t.Fatal(err)
	}
	after, _ := repo.SelectFormer5MinDeviceRealKpi(context.Background())
	if after[day] != before[day] {
		t.Fatalf("refetch changed the day's count from %d to %d", before[day], after[day])
	}
	if err := checkCollectionProgress(day, before[day], after[day]); err == nil {
		t.Fatal("expected an error for a run that stored no new rows")
	}
}

func TestCheckCollectionProgress(t *testing.T) {
	day := time.Date(2026, 1, 31, 0, 0, 0, 0, time.UTC)
	if err := checkCollectionProgress(day, 0, 0); err == nil || !strings.Contains(err.Error(), "2026-01-31") {
		t.Errorf("no rows: expected an error naming the day, got %v", err)
	}
	if err := checkCollectionProgress(day, 0, 50); err != nil {
		t.Errorf("incomplete day with new rows: expected only a warning, got %v", err)
	}
	if err := checkCollectionProgress(day, 50, 560); err != nil {
		t.Errorf("completed day: expected no error, got %v", err)
	}
}

// The day still running is never fetched: after a few hours it would count as
// collected and its rest would never be fetched.
func TestDetermineNextCollectionDate_SkipsTheRunningDay(t *testing.T) {
	today := time.Date(2026, 11, 19, 0, 0, 0, 0, time.UTC)
	records := completeUntil(today.Add(-24 * time.Hour))
	if next := determineNextCollectionDate(records, today.Add(15*time.Hour)); next != nil {
		t.Errorf("expected nil while today is running, got %v", *next)
	}
	if next := determineNextCollectionDate(records, today.Add(24*time.Hour)); next == nil || !next.Equal(today) {
		t.Errorf("expected %v once the day is over, got %v", today, next)
	}
}
