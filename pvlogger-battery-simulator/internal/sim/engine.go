package sim

import "time"

// Sample is one measured row from house_power_kpi_values. Powers in kW.
type Sample struct {
	Time           time.Time
	SolarKW        float64
	SolarToHouseKW float64
	GridToHouseKW  float64
	SolarToGridKW  float64
}

// Totals accumulates energies (kWh) over a period, both for the measured
// baseline and for the simulated battery operation.
type Totals struct {
	ConsumptionKWh  float64 // house consumption = solar_to_house + grid_to_house
	ProductionKWh   float64 // PV yield
	SolarToHouseKWh float64 // directly self-consumed PV (baseline)
	ImportKWh       float64 // baseline grid import
	ExportKWh       float64 // baseline grid export

	AvoidedImportKWh float64 // battery discharge delivered to the house (AC)
	ReducedExportKWh float64 // PV surplus diverted into the battery (AC)
	ChargedKWh       float64 // energy stored in cells (after charge losses)
	DischargedKWh    float64 // energy drawn from cells (before discharge losses)

	CoveredHours  float64
	ExcludedHours float64 // time skipped because the gap exceeded maxGap
	Samples       int
	GapsSkipped   int
}

func (t *Totals) add(o *Totals) {
	t.ConsumptionKWh += o.ConsumptionKWh
	t.ProductionKWh += o.ProductionKWh
	t.SolarToHouseKWh += o.SolarToHouseKWh
	t.ImportKWh += o.ImportKWh
	t.ExportKWh += o.ExportKWh
	t.AvoidedImportKWh += o.AvoidedImportKWh
	t.ReducedExportKWh += o.ReducedExportKWh
	t.ChargedKWh += o.ChargedKWh
	t.DischargedKWh += o.DischargedKWh
	t.CoveredHours += o.CoveredHours
	t.ExcludedHours += o.ExcludedHours
	t.Samples += o.Samples
	t.GapsSkipped += o.GapsSkipped
}

// Engine simulates one battery over a chronological stream of samples.
// Each interval is attributed to the month of its end sample.
type Engine struct {
	Battery Battery

	socKWh  float64
	maxGapH float64
	loc     *time.Location

	prev    time.Time
	hasPrev bool

	Total   Totals
	Monthly map[string]*Totals // keyed "2024-01"
}

func NewEngine(b Battery, maxGap time.Duration, loc *time.Location) *Engine {
	return &Engine{
		Battery: b,
		socKWh:  b.StartSoC * b.CapacityKWh,
		maxGapH: maxGap.Hours(),
		loc:     loc,
		Monthly: map[string]*Totals{},
	}
}

// SoCKWh exposes the current state of charge (cell side).
func (e *Engine) SoCKWh() float64 { return e.socKWh }

func (e *Engine) Step(s Sample) {
	if !e.hasPrev {
		e.prev, e.hasPrev = s.Time, true
		return
	}
	dtH := s.Time.Sub(e.prev).Hours()
	e.prev = s.Time
	if dtH <= 0 {
		return
	}

	month := s.Time.In(e.loc).Format("2006-01")
	bucket := e.Monthly[month]
	if bucket == nil {
		bucket = &Totals{}
		e.Monthly[month] = bucket
	}

	if dtH > e.maxGapH {
		// Collector outage: hold SoC, count the time as not simulated.
		step := Totals{ExcludedHours: dtH, GapsSkipped: 1}
		bucket.add(&step)
		e.Total.add(&step)
		return
	}

	b := e.Battery
	step := Totals{
		ConsumptionKWh:  (s.SolarToHouseKW + s.GridToHouseKW) * dtH,
		ProductionKWh:   s.SolarKW * dtH,
		SolarToHouseKWh: s.SolarToHouseKW * dtH,
		ImportKWh:       s.GridToHouseKW * dtH,
		ExportKWh:       s.SolarToGridKW * dtH,
		CoveredHours:    dtH,
		Samples:         1,
	}

	// Charge from PV surplus that was measured as grid export.
	if s.SolarToGridKW > 0 {
		acOffered := min(s.SolarToGridKW, b.MaxChargeKW) * dtH
		headroom := b.CapacityKWh - e.socKWh
		acAccepted := min(acOffered, headroom/b.ChargeEff)
		if acAccepted > 0 {
			stored := acAccepted * b.ChargeEff
			e.socKWh += stored
			step.ReducedExportKWh = acAccepted
			step.ChargedKWh = stored
		}
	}

	// Discharge to cover demand that was measured as grid import.
	if s.GridToHouseKW > 0 {
		acWanted := min(s.GridToHouseKW, b.MaxDischargeKW) * dtH
		available := max(0, e.socKWh-b.ReserveSoC*b.CapacityKWh)
		acDelivered := min(acWanted, available*b.DischargeEff)
		if acDelivered > 0 {
			drawn := acDelivered / b.DischargeEff
			e.socKWh -= drawn
			step.AvoidedImportKWh = acDelivered
			step.DischargedKWh = drawn
		}
	}

	bucket.add(&step)
	e.Total.add(&step)
}
