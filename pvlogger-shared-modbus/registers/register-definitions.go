// Package registers holds the deliberately hard-coded KPI register set
// (ADR-0004 speed rationale, kept by ADR-0016): the fast collector reads these
// without a masterdata dependency, and the shared KPI aggregation path
// (package aggregate) uses the same definitions to read a run's values back
// from the data accessor. Unlike the fast collector's previous private copy,
// the definitions are wire-complete — every field the Kotlin side requires is
// set with its enum serial name — because the dongle block is POSTed to the
// data accessor's generic register-value store.
package registers

import "pvlogger-shared-modbus/model"

// InverterDailyYield - Register 32114 (U32, 2 registers, kWh, gain 100)
// Daily energy yield, resets each day.
var InverterDailyYield = model.RegisterDefinition{
	No:              32114,
	Name:            "Daily energy yield",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "KWH",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 32114},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// InverterAccumulatedYield - Register 32106 (U32, 2 registers, kWh, gain 100)
// Lifetime energy yield, monotonic. Daily production is derived from this
// counter as a day-delta (ADR-0014) — unlike register 32114, it has no
// midnight reset the collector could race against.
var InverterAccumulatedYield = model.RegisterDefinition{
	No:              32106,
	Name:            "Accumulated energy yield",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "KWH",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 32106},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// MeterExportedEnergy - Register 37119 (U32, 2 registers, kWh, gain 100)
// Cumulative energy exported to the grid.
var MeterExportedEnergy = model.RegisterDefinition{
	No:              37119,
	Name:            "Positive active electricity",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "KWH",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 37119},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// MeterImportedEnergy - Register 37121 (U32, 2 registers, kWh, gain 100)
// Cumulative energy imported from the grid.
var MeterImportedEnergy = model.RegisterDefinition{
	No:              37121,
	Name:            "Reverse active power",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "KWH",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 37121},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// SmartDongle aggregate block (backlog 33): the dongle's own logical device
// (unit 100, see DONGLE_DEVICE_ID) serves values it computes over all cascaded
// inverters and the meter. The block MUST be read in a single request
// (37498 x20) so all values describe one coherent snapshot — that coherence is
// the whole point (live-KPI skew fix). Register names/types follow the
// wlcrs/huawei-solar-lib register table; the block is absent from Huawei's
// inverter interface PDF and from the masterdata catalog (backlog item 35).
// Verified live 2026-08-13 (night) and 2026-08-14 (day, exporting):
// total_active + grid = load held exactly in every sample; refresh ~10 s.

// DongleTotalInputPower - Register 37498 (U32, 2 registers, W, gain 1)
// DC input power over all inverters. Read as part of the block; stored in the
// generic register store but not used for KPIs.
var DongleTotalInputPower = model.RegisterDefinition{
	No:              37498,
	Name:            "Total input power",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37498},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// DongleLoadPower - Register 37500 (U32, 2 registers, W, gain 1)
// House consumption, measured directly — replaces the Consuming formula.
var DongleLoadPower = model.RegisterDefinition{
	No:              37500,
	Name:            "Load power",
	ReadWrite:       "RO",
	Type:            "U32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37500},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// DongleGridPower - Register 37502 (I32, 2 registers, W, gain 1)
// Grid exchange. Positive = import — the sign is INVERTED vs meter register
// 37113 (positive = export); the KPI aggregation flips it to the meter
// convention.
var DongleGridPower = model.RegisterDefinition{
	No:              37502,
	Name:            "Grid power",
	ReadWrite:       "RO",
	Type:            "I32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37502},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// DongleBatteryPower - Register 37504 (I32, 2 registers, W, gain 1)
// Battery charge/discharge power. No battery installed today (reads 0); comes
// for free inside the block and matters for the ADR-0006 re-evaluation.
var DongleBatteryPower = model.RegisterDefinition{
	No:              37504,
	Name:            "Battery power",
	ReadWrite:       "RO",
	Type:            "I32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37504},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// DongleReservedBlock - Registers 37506–37515 (10 registers, undocumented)
// Filler so the block request stays one contiguous read; decoded as MLD
// (raw shorts), never stored (see WithoutReservedBlock).
var DongleReservedBlock = model.RegisterDefinition{
	No:              37506,
	Name:            "Reserved",
	ReadWrite:       "RO",
	Type:            "MLD",
	Unit:            "NA",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37506},
	Quantity:        model.Quantity{Value: 10},
	Category:        "DYNAMIC",
}

// DongleTotalActivePower - Register 37516 (I32, 2 registers, W, gain 1)
// AC active power summed over all cascaded inverters — the Producing source
// (also removes the previous per-inverter 32080 double-read). Carried as kW
// after the THOUSAND gain, matching the raw row's inverterActivePower column.
var DongleTotalActivePower = model.RegisterDefinition{
	No:              37516,
	Name:            "Total active power",
	ReadWrite:       "RO",
	Type:            "I32",
	Unit:            "KW",
	Gain:            "THOUSAND",
	RegisterAddress: model.RegisterAddress{Value: 37516},
	Quantity:        model.Quantity{Value: 2},
	Category:        "DYNAMIC",
}

// DongleBlockRegisters returns the SmartDongle aggregate block (37498 x20) as
// one contiguous definition list — a single Modbus request, one snapshot.
func DongleBlockRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{
		DongleTotalInputPower,
		DongleLoadPower,
		DongleGridPower,
		DongleBatteryPower,
		DongleReservedBlock,
		DongleTotalActivePower,
	}
}

// InverterDailyYieldRegisters returns the daily yield power register definition
func InverterDailyYieldRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{InverterDailyYield}
}

// InverterAccumulatedYieldRegisters returns the lifetime yield register definition
func InverterAccumulatedYieldRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{InverterAccumulatedYield}
}

// MeterEnergyRegisters returns the meter energy registers (consecutive U32 block: 37119 + 37121).
func MeterEnergyRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{MeterExportedEnergy, MeterImportedEnergy}
}
