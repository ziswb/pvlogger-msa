package sim

import (
	"fmt"
	"sort"
)

// Battery describes one storage system variant to simulate.
// Efficiencies are per direction; round-trip = ChargeEff * DischargeEff.
type Battery struct {
	Name           string
	CapacityKWh    float64
	MaxChargeKW    float64
	MaxDischargeKW float64
	ChargeEff      float64 // AC -> cell
	DischargeEff   float64 // cell -> AC
	ReserveSoC     float64 // fraction of capacity never discharged
	StartSoC       float64 // fraction of capacity at simulation start
}

// UsableKWh is the capacity available above the reserve floor.
func (b Battery) UsableKWh() float64 {
	return b.CapacityKWh * (1 - b.ReserveSoC)
}

// Presets for common systems. LUNA2000 modules are 5 kWh each with 2.5 kW
// charge/discharge power per module, one power module (max 5 kW) per stack;
// 20 kWh means two stacks (one per SUN2000-8KTL), doubling the power.
// Per-direction efficiency 0.95 models the ~90% round-trip of LFP + DC/DC.
var presets = map[string]Battery{
	"luna2000-5":  {Name: "LUNA2000-5", CapacityKWh: 5, MaxChargeKW: 2.5, MaxDischargeKW: 2.5, ChargeEff: 0.95, DischargeEff: 0.95, ReserveSoC: 0.05, StartSoC: 0.05},
	"luna2000-10": {Name: "LUNA2000-10", CapacityKWh: 10, MaxChargeKW: 5, MaxDischargeKW: 5, ChargeEff: 0.95, DischargeEff: 0.95, ReserveSoC: 0.05, StartSoC: 0.05},
	"luna2000-15": {Name: "LUNA2000-15", CapacityKWh: 15, MaxChargeKW: 5, MaxDischargeKW: 5, ChargeEff: 0.95, DischargeEff: 0.95, ReserveSoC: 0.05, StartSoC: 0.05},
	"luna2000-20": {Name: "LUNA2000-20 (2x10)", CapacityKWh: 20, MaxChargeKW: 10, MaxDischargeKW: 10, ChargeEff: 0.95, DischargeEff: 0.95, ReserveSoC: 0.05, StartSoC: 0.05},
}

// Preset returns a battery preset by key, or an error listing valid keys.
func Preset(key string) (Battery, error) {
	if b, ok := presets[key]; ok {
		return b, nil
	}
	keys := make([]string, 0, len(presets))
	for k := range presets {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return Battery{}, fmt.Errorf("unknown battery preset %q, available: %v", key, keys)
}
