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

// Print writes a comparison of all engines to w.
func Print(w io.Writer, engines []*sim.Engine, tariff Tariff) {
	if len(engines) == 0 {
		return
	}
	base := engines[0].Total // baseline columns are identical across engines

	fmt.Fprintf(w, "\nPeriod coverage: %.0f h simulated, %.0f h excluded (%d gaps), %d samples\n",
		base.CoveredHours, base.ExcludedHours, base.GapsSkipped, base.Samples)
	fmt.Fprintf(w, "Baseline: production %.0f kWh, consumption %.0f kWh, import %.0f kWh, export %.0f kWh\n",
		base.ProductionKWh, base.ConsumptionKWh, base.ImportKWh, base.ExportKWh)
	fmt.Fprintf(w, "Baseline: self-consumption %.1f %%, autarky %.1f %%\n\n",
		pct(base.SolarToHouseKWh, base.ProductionKWh), pct(base.SolarToHouseKWh, base.ConsumptionKWh))

	header := fmt.Sprintf("%-28s %12s %12s %12s %12s %10s %10s %11s %12s", "battery",
		"import kWh", "export kWh", "self-cons %", "autarky %", "cycles", "RT eff %", "saved/y", "ΔSoC kWh")
	fmt.Fprintln(w, header)

	for _, e := range engines {
		t := e.Total
		b := e.Battery
		newImport := t.ImportKWh - t.AvoidedImportKWh
		newExport := t.ExportKWh - t.ReducedExportKWh
		selfCons := pct(t.SolarToHouseKWh+t.ReducedExportKWh, t.ProductionKWh)
		autarky := pct(t.SolarToHouseKWh+t.AvoidedImportKWh, t.ConsumptionKWh)
		cycles := 0.0
		if b.UsableKWh() > 0 {
			cycles = t.DischargedKWh / b.UsableKWh()
		}
		savings := t.AvoidedImportKWh*tariff.ImportPrice - t.ReducedExportKWh*tariff.FeedInPrice
		deltaSoC := e.SoCKWh() - b.StartSoC*b.CapacityKWh

		fmt.Fprintf(w, "%-28s %12.0f %12.0f %12.1f %12.1f %10.0f %10.1f %11.0f %12.1f\n",
			b.Name, newImport, newExport, selfCons, autarky, cycles,
			pct(t.AvoidedImportKWh, t.ReducedExportKWh), savings, deltaSoC)
	}
	fmt.Fprintf(w, "\nEconomics assume import %.4f/kWh and feed-in %.4f/kWh; 'saved/y' scales with the simulated period.\n",
		tariff.ImportPrice, tariff.FeedInPrice)
}

// WriteMonthlyCSV exports per-month, per-battery results.
func WriteMonthlyCSV(path string, engines []*sim.Engine) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	defer f.Close()

	w := csv.NewWriter(f)
	defer w.Flush()

	if err := w.Write([]string{
		"month", "battery", "production_kwh", "consumption_kwh",
		"import_kwh", "export_kwh", "avoided_import_kwh", "reduced_export_kwh",
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
				f2(t.ImportKWh), f2(t.ExportKWh),
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
	return nil
}

func f2(v float64) string { return strconv.FormatFloat(v, 'f', 2, 64) }
