package sim

import (
	"math"
	"testing"
	"time"
)

var utc = time.UTC

func almostEqual(t *testing.T, name string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 1e-9 {
		t.Errorf("%s = %v, want %v", name, got, want)
	}
}

// feed generates n one-minute samples starting at start, all with the same powers.
func feed(e *Engine, start time.Time, n int, s Sample) time.Time {
	for i := 0; i <= n; i++ {
		s.Time = start.Add(time.Duration(i) * time.Minute)
		e.Step(s)
	}
	return start.Add(time.Duration(n) * time.Minute)
}

func TestChargeRespectsPowerLimitAndEfficiency(t *testing.T) {
	b := Battery{CapacityKWh: 10, MaxChargeKW: 2, MaxDischargeKW: 5, ChargeEff: 0.9, DischargeEff: 0.9, ReserveSoC: 0, StartSoC: 0}
	e := NewEngine(b, 5*time.Minute, utc)
	start := time.Date(2024, 6, 1, 10, 0, 0, 0, utc)

	// 5 kW surplus for 1h, charge power capped at 2 kW -> 2 kWh AC accepted
	feed(e, start, 60, Sample{SolarKW: 5, SolarToGridKW: 5})

	almostEqual(t, "ReducedExportKWh", e.Total.ReducedExportKWh, 2)
	almostEqual(t, "ChargedKWh", e.Total.ChargedKWh, 1.8)
	almostEqual(t, "SoCKWh", e.SoCKWh(), 1.8)
	almostEqual(t, "ExportKWh baseline", e.Total.ExportKWh, 5)
}

func TestChargeStopsAtFullCapacity(t *testing.T) {
	b := Battery{CapacityKWh: 3, MaxChargeKW: 10, MaxDischargeKW: 10, ChargeEff: 1, DischargeEff: 1, ReserveSoC: 0, StartSoC: 0}
	e := NewEngine(b, 5*time.Minute, utc)
	start := time.Date(2024, 6, 1, 10, 0, 0, 0, utc)

	// 10 kW surplus for 1h would be 10 kWh, but only 3 kWh fit
	feed(e, start, 60, Sample{SolarKW: 10, SolarToGridKW: 10})

	almostEqual(t, "SoCKWh", e.SoCKWh(), 3)
	almostEqual(t, "ReducedExportKWh", e.Total.ReducedExportKWh, 3)
}

func TestDischargeRespectsReserveAndEfficiency(t *testing.T) {
	b := Battery{CapacityKWh: 10, MaxChargeKW: 5, MaxDischargeKW: 4, ChargeEff: 1, DischargeEff: 0.8, ReserveSoC: 0.2, StartSoC: 1}
	e := NewEngine(b, 5*time.Minute, utc)
	start := time.Date(2024, 1, 15, 18, 0, 0, 0, utc)

	// 10 kW deficit for 3h; usable 8 kWh cells * 0.8 = 6.4 kWh AC
	feed(e, start, 180, Sample{GridToHouseKW: 10})

	almostEqual(t, "AvoidedImportKWh", e.Total.AvoidedImportKWh, 6.4)
	almostEqual(t, "DischargedKWh", e.Total.DischargedKWh, 8)
	almostEqual(t, "SoCKWh (reserve floor)", e.SoCKWh(), 2)
	almostEqual(t, "ImportKWh baseline", e.Total.ImportKWh, 30)
}

func TestDischargePowerLimit(t *testing.T) {
	b := Battery{CapacityKWh: 100, MaxChargeKW: 5, MaxDischargeKW: 2, ChargeEff: 1, DischargeEff: 1, ReserveSoC: 0, StartSoC: 1}
	e := NewEngine(b, 5*time.Minute, utc)
	start := time.Date(2024, 1, 15, 18, 0, 0, 0, utc)

	// 10 kW deficit for 1h, discharge capped at 2 kW -> only 2 kWh avoided
	feed(e, start, 60, Sample{GridToHouseKW: 10})

	almostEqual(t, "AvoidedImportKWh", e.Total.AvoidedImportKWh, 2)
}

func TestGapIsExcludedAndHoldsSoC(t *testing.T) {
	b := Battery{CapacityKWh: 10, MaxChargeKW: 5, MaxDischargeKW: 5, ChargeEff: 1, DischargeEff: 1, ReserveSoC: 0, StartSoC: 0.5}
	e := NewEngine(b, 5*time.Minute, utc)
	t0 := time.Date(2024, 3, 1, 10, 0, 0, 0, utc)

	e.Step(Sample{Time: t0, GridToHouseKW: 10})
	// 2h outage: no charge, no discharge, no baseline energy
	e.Step(Sample{Time: t0.Add(2 * time.Hour), GridToHouseKW: 10})

	almostEqual(t, "SoCKWh", e.SoCKWh(), 5)
	almostEqual(t, "ExcludedHours", e.Total.ExcludedHours, 2)
	almostEqual(t, "ImportKWh", e.Total.ImportKWh, 0)
	if e.Total.GapsSkipped != 1 {
		t.Errorf("GapsSkipped = %d, want 1", e.Total.GapsSkipped)
	}
}

func TestBaselineAccountingAndMonthlyBuckets(t *testing.T) {
	b := Battery{CapacityKWh: 10, MaxChargeKW: 5, MaxDischargeKW: 5, ChargeEff: 1, DischargeEff: 1, ReserveSoC: 0, StartSoC: 0}
	e := NewEngine(b, 5*time.Minute, utc)

	// last hour of January (intervals ending 23:00-23:59) + first hour of February
	end := feed(e, time.Date(2024, 1, 31, 22, 59, 0, 0, utc), 60,
		Sample{SolarKW: 0, SolarToHouseKW: 0, GridToHouseKW: 1})
	feed(e, end, 60, Sample{SolarKW: 4, SolarToHouseKW: 3, GridToHouseKW: 0, SolarToGridKW: 1})

	almostEqual(t, "ConsumptionKWh", e.Total.ConsumptionKWh, 1+3)
	almostEqual(t, "ProductionKWh", e.Total.ProductionKWh, 4)
	almostEqual(t, "SolarToHouseKWh", e.Total.SolarToHouseKWh, 3)
	if len(e.Monthly) != 2 {
		t.Fatalf("Monthly buckets = %d, want 2 (%v)", len(e.Monthly), e.Monthly)
	}
	almostEqual(t, "January import", e.Monthly["2024-01"].ImportKWh, 1)
	almostEqual(t, "February export", e.Monthly["2024-02"].ExportKWh, 1)
}

func TestFullCycleRoundTrip(t *testing.T) {
	b := Battery{CapacityKWh: 10, MaxChargeKW: 10, MaxDischargeKW: 10, ChargeEff: 0.95, DischargeEff: 0.95, ReserveSoC: 0, StartSoC: 0}
	e := NewEngine(b, 5*time.Minute, utc)
	start := time.Date(2024, 6, 1, 8, 0, 0, 0, utc)

	// charge fully (12 kWh AC offered, 10/0.95 = 10.526 accepted), then drain fully
	end := feed(e, start, 120, Sample{SolarKW: 6, SolarToGridKW: 6})
	feed(e, end, 240, Sample{GridToHouseKW: 6})

	roundTrip := e.Total.AvoidedImportKWh / e.Total.ReducedExportKWh
	almostEqual(t, "round trip", roundTrip, 0.95*0.95)
	almostEqual(t, "SoCKWh drained", e.SoCKWh(), 0)
}
