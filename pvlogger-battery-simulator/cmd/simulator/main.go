package main

import (
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"pv-logger/logging"

	"pvlogger-battery-simulator/internal/database"
	"pvlogger-battery-simulator/internal/report"
	"pvlogger-battery-simulator/internal/sim"
)

type batteryList []string

func (b *batteryList) String() string     { return strings.Join(*b, ",") }
func (b *batteryList) Set(v string) error { *b = append(*b, v); return nil }

func main() {
	var batteries batteryList
	fromStr := flag.String("from", "", "start date, e.g. 2024-01-01 (local time, inclusive)")
	toStr := flag.String("to", "", "end date, e.g. 2025-01-01 (local time, exclusive)")
	flag.Var(&batteries, "battery", "battery preset, repeatable (default: luna2000-15, luna2000-20)")
	maxGap := flag.Duration("max-gap", 5*time.Minute, "intervals longer than this are excluded (SoC held)")
	importPrice := flag.Float64("import-price", 0.2576, "grid import price per kWh (flat tariff)")
	feedInPrice := flag.Float64("feedin-price", 0.06, "feed-in remuneration per kWh")
	csvPath := flag.String("csv", "", "optional path for a per-month CSV export")
	summaryCSVPath := flag.String("summary-csv", "", "optional path for a whole-period summary CSV (one row per battery)")
	dbHost := flag.String("db-host", "<central-host>", "PostgreSQL host")
	dbPort := flag.Int("db-port", 5434, "PostgreSQL port")
	dbName := flag.String("db-name", "pvdata", "database name")
	dbUser := flag.String("db-user", "battery_sim_ro", "database user (password via PGPASSWORD)")
	dbSslMode := flag.String("db-sslmode", "prefer", "TLS to PostgreSQL: prefer falls back to plaintext, require does not")
	flag.Parse()

	if len(batteries) == 0 {
		batteries = batteryList{"luna2000-15", "luna2000-20"}
	}
	password := os.Getenv("PGPASSWORD")
	if password == "" {
		logging.Fatal("PGPASSWORD environment variable is not set")
	}

	loc, err := time.LoadLocation("Europe/Zurich")
	if err != nil {
		logging.Fatal("failed to load timezone", "error", err)
	}
	from, to, err := parsePeriod(*fromStr, *toStr, loc)
	if err != nil {
		logging.Fatal("invalid period", "error", err)
	}

	engines := make([]*sim.Engine, 0, len(batteries))
	for _, key := range batteries {
		b, err := sim.Preset(key)
		if err != nil {
			logging.Fatal("invalid battery", "error", err)
		}
		engines = append(engines, sim.NewEngine(b, *maxGap, loc))
	}

	db, err := database.NewDatabase(*dbHost, *dbPort, *dbName, *dbUser, password, *dbSslMode)
	if err != nil {
		logging.Fatal("failed to connect to database", "error", err)
	}
	defer func() {
		if err := db.Close(); err != nil {
			logging.Error("failed to close database connection", "error", err)
		}
	}()

	logging.Info("simulating", "from", from, "to", to, "batteries", batteries.String())
	count, err := db.StreamSamples(from, to, func(s sim.Sample) {
		for _, e := range engines {
			e.Step(s)
		}
	})
	if err != nil {
		logging.Fatal("failed to read samples", "error", err)
	}
	if count == 0 {
		logging.Fatal("no samples found in the requested period")
	}

	fmt.Printf("Simulated %s to %s\n", from.Format("2006-01-02"), to.Format("2006-01-02"))
	report.Print(os.Stdout, engines, report.Tariff{ImportPrice: *importPrice, FeedInPrice: *feedInPrice})

	if *csvPath != "" {
		if err := report.WriteMonthlyCSV(*csvPath, engines); err != nil {
			logging.Fatal("failed to write CSV", "error", err)
		}
		logging.Info("monthly CSV written", "path", *csvPath)
	}
	if *summaryCSVPath != "" {
		if err := report.WriteSummaryCSV(*summaryCSVPath, engines, report.Tariff{ImportPrice: *importPrice, FeedInPrice: *feedInPrice}); err != nil {
			logging.Fatal("failed to write summary CSV", "error", err)
		}
		logging.Info("summary CSV written", "path", *summaryCSVPath)
	}
}

func parsePeriod(fromStr, toStr string, loc *time.Location) (time.Time, time.Time, error) {
	if fromStr == "" || toStr == "" {
		return time.Time{}, time.Time{}, fmt.Errorf("both -from and -to are required (e.g. -from 2024-01-01 -to 2025-01-01)")
	}
	from, err := time.ParseInLocation("2006-01-02", fromStr, loc)
	if err != nil {
		return time.Time{}, time.Time{}, err
	}
	to, err := time.ParseInLocation("2006-01-02", toStr, loc)
	if err != nil {
		return time.Time{}, time.Time{}, err
	}
	if !to.After(from) {
		return time.Time{}, time.Time{}, fmt.Errorf("-to must be after -from")
	}
	return from, to, nil
}
