package registers

import "pvlogger-shared-modbus/model"

// InverterDailyYield - Register 32114 (U32, 2 registers, kWh, gain 100)
// Daily energy yield, resets each day.
var InverterDailyYield = model.RegisterDefinition{
	Name:            "Daily energy yield",
	Type:            "U32",
	Unit:            "kWh",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 32114},
	Quantity:        model.Quantity{Value: 2},
}

// InverterAccumulatedYield - Register 32106 (U32, 2 registers, kWh, gain 100)
// Lifetime energy yield, monotonic. Daily production is derived from this
// counter as a day-delta (ADR-0014) — unlike register 32114, it has no
// midnight reset the collector could race against.
var InverterAccumulatedYield = model.RegisterDefinition{
	Name:            "Accumulated energy yield",
	Type:            "U32",
	Unit:            "kWh",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 32106},
	Quantity:        model.Quantity{Value: 2},
}

// MeterExportedEnergy - Register 37119 (U32, 2 registers, kWh, gain 100)
// Cumulative energy exported to the grid.
var MeterExportedEnergy = model.RegisterDefinition{
	Name:            "Positive active electricity",
	Type:            "U32",
	Unit:            "kWh",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 37119},
	Quantity:        model.Quantity{Value: 2},
}

// MeterImportedEnergy - Register 37121 (U32, 2 registers, kWh, gain 100)
// Cumulative energy imported from the grid.
var MeterImportedEnergy = model.RegisterDefinition{
	Name:            "Reverse active power",
	Type:            "U32",
	Unit:            "kWh",
	Gain:            "HUNDRED",
	RegisterAddress: model.RegisterAddress{Value: 37121},
	Quantity:        model.Quantity{Value: 2},
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
// DC input power over all inverters. Read as part of the block, unused.
var DongleTotalInputPower = model.RegisterDefinition{
	Name:            "Total input power",
	Type:            "U32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37498},
	Quantity:        model.Quantity{Value: 2},
}

// DongleLoadPower - Register 37500 (U32, 2 registers, W, gain 1)
// House consumption, measured directly — replaces the Consuming formula.
var DongleLoadPower = model.RegisterDefinition{
	Name:            "Load power",
	Type:            "U32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37500},
	Quantity:        model.Quantity{Value: 2},
}

// DongleGridPower - Register 37502 (I32, 2 registers, W, gain 1)
// Grid exchange. Positive = import — the sign is INVERTED vs meter register
// 37113 (positive = export); the collector flips it to the meter convention.
var DongleGridPower = model.RegisterDefinition{
	Name:            "Grid power",
	Type:            "I32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37502},
	Quantity:        model.Quantity{Value: 2},
}

// DongleBatteryPower - Register 37504 (I32, 2 registers, W, gain 1)
// Battery charge/discharge power. No battery installed today (reads 0); comes
// for free inside the block and matters for the ADR-0006 re-evaluation.
var DongleBatteryPower = model.RegisterDefinition{
	Name:            "Battery power",
	Type:            "I32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37504},
	Quantity:        model.Quantity{Value: 2},
}

// DongleReservedBlock - Registers 37506–37515 (10 registers, undocumented)
// Filler so the block request stays one contiguous read; decoded as MLD
// (raw shorts) and ignored.
var DongleReservedBlock = model.RegisterDefinition{
	Name:            "Reserved",
	Type:            "MLD",
	Unit:            "",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37506},
	Quantity:        model.Quantity{Value: 10},
}

// DongleTotalActivePower - Register 37516 (I32, 2 registers, W, gain 1)
// AC active power summed over all cascaded inverters — the Producing source
// (also removes the previous per-inverter 32080 double-read). Stored in the
// raw row's inverterActivePower column, which carries kW, hence gain 1000.
var DongleTotalActivePower = model.RegisterDefinition{
	Name:            "Total active power",
	Type:            "I32",
	Unit:            "kW",
	Gain:            "THOUSAND",
	RegisterAddress: model.RegisterAddress{Value: 37516},
	Quantity:        model.Quantity{Value: 2},
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
