package report

import (
	"encoding/csv"
	"fmt"
	"io"
	"os"
	"sort"
	"strconv"

	"pvlogger-battery-simulator/internal/sim"
)

// Tariff holds the prices used for the economics section (currency/kWh).
type Tariff struct {
	ImportPrice float64
	FeedInPrice float64
}

func pct(part, whole float64) float64 {
	if whole == 0 {
		return 0
	}
	return 100 * part / whole
}

// derived are the per-battery figures shown in the console table and the
// summary CSV, computed from an engine's totals.
type derived struct {
	ImportNoBatteryKWh   float64
	ExportNoBatteryKWh   float64
	ImportWithBatteryKWh float64
	ExportWithBatteryKWh float64
	SelfConsPct          float64
	AutarkyPct           float64
	BaseSelfConsPct      float64
	BaseAutarkyPct       float64
	Cycles               float64
	RoundTripPct         float64
	Savings              float64
	DeltaSoCKWh          float64
}

func derive(e *sim.Engine, tariff Tariff) derived {
	t := e.Total
	b := e.Battery
	d := derived{
		ImportNoBatteryKWh:   t.ImportKWh,
		ExportNoBatteryKWh:   t.ExportKWh,
		ImportWithBatteryKWh: t.ImportKWh - t.AvoidedImportKWh,
		ExportWithBatteryKWh: t.ExportKWh - t.ReducedExportKWh,
		SelfConsPct:          pct(t.SolarToHouseKWh+t.ReducedExportKWh, t.ProductionKWh),
		AutarkyPct:           pct(t.SolarToHouseKWh+t.AvoidedImportKWh, t.ConsumptionKWh),
		BaseSelfConsPct:      pct(t.SolarToHouseKWh, t.ProductionKWh),
		BaseAutarkyPct:       pct(t.SolarToHouseKWh, t.ConsumptionKWh),
		RoundTripPct:         pct(t.AvoidedImportKWh, t.ReducedExportKWh),
		Savings:              t.AvoidedImportKWh*tariff.ImportPrice - t.ReducedExportKWh*tariff.FeedInPrice,
		DeltaSoCKWh:          e.SoCKWh() - b.StartSoC*b.CapacityKWh,
	}
	if b.UsableKWh() > 0 {
		d.Cycles = t.DischargedKWh / b.UsableKWh()
	}
	return d
}

// Print writes a comparison of all engines to w.
func Print(w io.Writer, engines []*sim.Engine, tariff Tariff) {
	if len(engines) == 0 {
		return
	}
	base := engines[0].Total // baseline columns are identical across engines
	baseD := derive(engines[0], tariff)

	_, _ = fmt.Fprintf(w, "\nPeriod coverage: %.0f h simulated, %.0f h excluded (%d gaps), %d samples\n",
		base.CoveredHours, base.ExcludedHours, base.GapsSkipped, base.Samples)
	_, _ = fmt.Fprintf(w, "Baseline: production %.0f kWh, consumption %.0f kWh, import %.0f kWh, export %.0f kWh\n",
		base.ProductionKWh, base.ConsumptionKWh, base.ImportKWh, base.ExportKWh)
	_, _ = fmt.Fprintf(w, "Baseline: self-consumption %.1f %%, autarky %.1f %%\n\n",
		baseD.BaseSelfConsPct, baseD.BaseAutarkyPct)

	header := fmt.Sprintf("%-28s %12s %12s %12s %12s %10s %10s %11s %12s", "battery",
		"import kWh", "export kWh", "self-cons %", "autarky %", "cycles", "RT eff %", "saved/y", "ΔSoC kWh")
	_, _ = fmt.Fprintln(w, header)

	for _, e := range engines {
		d := derive(e, tariff)
		_, _ = fmt.Fprintf(w, "%-28s %12.0f %12.0f %12.1f %12.1f %10.0f %10.1f %11.0f %12.1f\n",
			e.Battery.Name, d.ImportWithBatteryKWh, d.ExportWithBatteryKWh, d.SelfConsPct, d.AutarkyPct,
			d.Cycles, d.RoundTripPct, d.Savings, d.DeltaSoCKWh)
	}
	_, _ = fmt.Fprintf(w, "\nEconomics assume import %.4f/kWh and feed-in %.4f/kWh; 'saved/y' scales with the simulated period.\n",
		tariff.ImportPrice, tariff.FeedInPrice)
}

// WriteSummaryCSV exports the whole-period comparison (one row per battery),
// mirroring the console table plus the baseline figures.
func WriteSummaryCSV(path string, engines []*sim.Engine, tariff Tariff) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	defer func() { _ = f.Close() }()

	w := csv.NewWriter(f)

	if err := w.Write([]string{
		"battery", "capacity_kwh",
		"production_kwh", "consumption_kwh",
		"import_no_battery_kwh", "export_no_battery_kwh",
		"import_with_battery_kwh", "export_with_battery_kwh",
		"avoided_import_kwh", "reduced_export_kwh",
		"self_consumption_no_battery_pct", "self_consumption_pct",
		"autarky_no_battery_pct", "autarky_pct",
		"full_cycles", "round_trip_pct", "saved",
		"delta_soc_kwh", "covered_hours", "excluded_hours",
		"import_price", "feedin_price",
	}); err != nil {
		return err
	}
	for _, e := range engines {
		t := e.Total
		d := derive(e, tariff)
		rec := []string{
			e.Battery.Name, f2(e.Battery.CapacityKWh),
			f2(t.ProductionKWh), f2(t.ConsumptionKWh),
			f2(d.ImportNoBatteryKWh), f2(d.ExportNoBatteryKWh),
			f2(d.ImportWithBatteryKWh), f2(d.ExportWithBatteryKWh),
			f2(t.AvoidedImportKWh), f2(t.ReducedExportKWh),
			f2(d.BaseSelfConsPct), f2(d.SelfConsPct),
			f2(d.BaseAutarkyPct), f2(d.AutarkyPct),
			f2(d.Cycles), f2(d.RoundTripPct), f2(d.Savings),
			f2(d.DeltaSoCKWh), f2(t.CoveredHours), f2(t.ExcludedHours),
			f4(tariff.ImportPrice), f4(tariff.FeedInPrice),
		}
		if err := w.Write(rec); err != nil {
			return err
		}
	}
	w.Flush()
	if err := w.Error(); err != nil {
		return err
	}
	return f.Close()
}

// WriteMonthlyCSV exports per-month, per-battery results.
func WriteMonthlyCSV(path string, engines []*sim.Engine) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	defer func() { _ = f.Close() }()

	w := csv.NewWriter(f)

	if err := w.Write([]string{
		"month", "battery", "production_kwh", "consumption_kwh",
		"import_no_battery_kwh", "import_with_battery_kwh",
		"export_no_battery_kwh", "export_with_battery_kwh",
		"avoided_import_kwh", "reduced_export_kwh",
		"self_consumption_pct", "autarky_pct", "excluded_hours",
	}); err != nil {
		return err
	}
	for _, e := range engines {
		months := make([]string, 0, len(e.Monthly))
		for m := range e.Monthly {
			months = append(months, m)
		}
		sort.Strings(months)
		for _, m := range months {
			t := e.Monthly[m]
			rec := []string{
				m, e.Battery.Name,
				f2(t.ProductionKWh), f2(t.ConsumptionKWh),
				f2(t.ImportKWh), f2(t.ImportKWh - t.AvoidedImportKWh),
				f2(t.ExportKWh), f2(t.ExportKWh - t.ReducedExportKWh),
				f2(t.AvoidedImportKWh), f2(t.ReducedExportKWh),
				f2(pct(t.SolarToHouseKWh+t.ReducedExportKWh, t.ProductionKWh)),
				f2(pct(t.SolarToHouseKWh+t.AvoidedImportKWh, t.ConsumptionKWh)),
				f2(t.ExcludedHours),
			}
			if err := w.Write(rec); err != nil {
				return err
			}
		}
	}
	w.Flush()
	if err := w.Error(); err != nil {
		return err
	}
	return f.Close()
}

func f2(v float64) string { return strconv.FormatFloat(v, 'f', 2, 64) }
func f4(v float64) string { return strconv.FormatFloat(v, 'f', 4, 64) }
