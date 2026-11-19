package repository

import (
	"context"
	"os"
	"testing"
	"time"
	_ "time/tzdata" // the test sets a site zone; not every CI image ships zoneinfo

	"pvlogger-fusionsolar/internal/database"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
)

// openTestRepository connects to the throwaway PostgreSQL named by
// FSDATA_TEST_DSN (CI: a postgres:18 service in go.yml), applies the fsdata
// migrations and empties the tables the test writes. Without the variable the
// test is skipped.
func openTestRepository(t *testing.T) *PostgresRepository {
	t.Helper()
	dsn := os.Getenv("FSDATA_TEST_DSN")
	if dsn == "" {
		t.Skip("FSDATA_TEST_DSN not set: no test database")
	}
	ctx := context.Background()
	conn, err := pgx.Connect(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = conn.Close(ctx) })
	if _, _, err := database.Migrate(ctx, conn); err != nil {
		t.Fatal(err)
	}
	if _, err := conn.Exec(ctx, "truncate fs_device_real_kpi, fs_response cascade"); err != nil {
		t.Fatal(err)
	}
	return NewPostgresRepository(&database.Database{Conn: conn, Ctx: ctx})
}

// The per-day count is computed by PostgreSQL since backlog 66 (the per-row
// read was OOMKilled). It must count what the Go code counted before: rows of
// type DEVICE_5_MIN_KPI with a collect_time, distinct per device and time,
// per day in the process's zone, keyed as midnight UTC.
func TestSelectFormer5MinDeviceRealKpi_CountsPerSiteDay(t *testing.T) {
	zone, err := time.LoadLocation("Europe/Berlin")
	if err != nil {
		t.Fatal(err)
	}
	old := time.Local
	time.Local = zone
	t.Cleanup(func() { time.Local = old })

	repo := openTestRepository(t)
	ctx := context.Background()
	responseID, err := repo.InsertFsResponse(ctx, FsResponseInput{
		Raw: []byte("{}"), Success: true, Timestamp: pgtype.Timestamp{Valid: true, Time: time.Now()},
	})
	if err != nil {
		t.Fatal(err)
	}
	insert := func(typ string, dev int64, collectTime *time.Time) {
		t.Helper()
		input := DeviceRealKpiInput{Response: responseID, Type: typ, DevId: pgtype.Int8{Valid: true, Int64: dev}}
		if collectTime != nil {
			input.CollectTime = pgtype.Int8{Valid: true, Int64: collectTime.UnixMilli()}
		}
		if err := repo.InsertDeviceRealKpi(ctx, input); err != nil {
			t.Fatal(err)
		}
	}
	at := func(s string) *time.Time {
		ts, err := time.Parse(time.RFC3339, s)
		if err != nil {
			t.Fatal(err)
		}
		return &ts
	}

	points := []*time.Time{
		at("2026-01-30T23:30:00Z"), // 00:30 CET on 01-31: the site day, not the UTC day
		at("2026-01-31T12:00:00Z"),
		at("2026-01-31T22:55:00Z"), // 23:55 CET, still 01-31
		at("2026-01-31T23:05:00Z"), // 00:05 CET on 02-01
		at("2026-07-15T22:30:00Z"), // 00:30 CEST on 07-16
	}
	var stored []int64
	for _, dev := range []int64{1, 2} {
		for _, p := range points {
			insert("DEVICE_5_MIN_KPI", dev, p)
			stored = append(stored, p.UnixMilli())
		}
	}
	insert("DEVICE_5_MIN_KPI", 1, points[1]) // a refetch: counts once
	insert("DEVICE_5_MIN_KPI", 2, points[1])
	insert("DEVICE_5_MIN_KPI", 1, nil)                       // no collect_time: ignored
	insert("DEVICE_REAL_KPI", 1, points[2])                  // real-time KPI: ignored
	insert("DEVICE_REAL_KPI", 1, nil)                        // as devkpi stores them
	insert("DEVICE_REAL_KPI", 3, at("2026-03-01T10:00:00Z")) // ignored, no day of its own

	got, err := repo.SelectFormer5MinDeviceRealKpi(ctx)
	if err != nil {
		t.Fatal(err)
	}
	day := func(y int, m time.Month, d int) time.Time { return time.Date(y, m, d, 0, 0, 0, 0, time.UTC) }
	want := map[time.Time]int{
		day(2026, 1, 31): 6,
		day(2026, 2, 1):  2,
		day(2026, 7, 16): 2,
	}
	// The same, counted the way the Go code did before the query aggregated.
	before := map[time.Time]int{}
	for _, ms := range stored {
		ts := time.UnixMilli(ms)
		before[time.Date(ts.Year(), ts.Month(), ts.Day(), 0, 0, 0, 0, time.UTC)]++
	}
	for name, expected := range map[string]map[time.Time]int{"expected": want, "previous Go counting": before} {
		if len(got) != len(expected) {
			t.Errorf("%s: got %v, want %v", name, got, expected)
			continue
		}
		for d, n := range expected {
			if got[d] != n {
				t.Errorf("%s: day %s counts %d, want %d (all: %v)", name, d.Format(time.DateOnly), got[d], n, got)
			}
		}
	}
}

func TestSiteZone(t *testing.T) {
	old := time.Local
	t.Cleanup(func() { time.Local = old })

	zone, err := time.LoadLocation("Europe/Berlin")
	if err != nil {
		t.Fatal(err)
	}
	time.Local = zone
	if got := SiteZone(); got != "Europe/Berlin" {
		t.Errorf("SiteZone() = %q, want Europe/Berlin", got)
	}
	time.Local = time.FixedZone("Local", 3600)
	if got := SiteZone(); got != "UTC" {
		t.Errorf("SiteZone() with an unnamed zone = %q, want UTC", got)
	}
}
