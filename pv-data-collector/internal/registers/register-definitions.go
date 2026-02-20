package registers

import "pv-data-collector/internal/modbus/model"

// InverterActivePower - Register 32080 (U32, 2 registers, kW, gain 1000)
// Active power currently being produced by the inverter.
var InverterActivePower = model.RegisterDefinition{
	Name:            "Active power",
	Type:            "U32",
	Unit:            "kW",
	Gain:            "THOUSAND",
	RegisterAddress: model.RegisterAddress{Value: 32080},
	Quantity:        model.Quantity{Value: 2},
}

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

// MeterActivePower - Register 37113 (I32, 2 registers, W, gain 1)
// Active power at the meter. Positive = export, negative = import.
var MeterActivePower = model.RegisterDefinition{
	Name:            "Active power",
	Type:            "I32",
	Unit:            "W",
	Gain:            "ONE",
	RegisterAddress: model.RegisterAddress{Value: 37113},
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

// InverterRegisters returns the register definitions to read per inverter device.
func InverterRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{InverterActivePower, InverterDailyYield}
}

// MeterActivePowerRegisters returns the meter active power register (read separately as I32).
func MeterActivePowerRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{MeterActivePower}
}

// MeterEnergyRegisters returns the meter energy registers (consecutive U32 block: 37119 + 37121).
func MeterEnergyRegisters() []model.RegisterDefinition {
	return []model.RegisterDefinition{MeterExportedEnergy, MeterImportedEnergy}
}
