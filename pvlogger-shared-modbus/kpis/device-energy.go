package kpis

type InverterActivePower struct {
	producingValue float64
	sourceUnit     Unit
}

func NewInverterActivePower(value float64, unit Unit) *InverterActivePower {
	return &InverterActivePower{value, unit}
}

func NewMeterEnergyToday(importedValue float64, importUnit Unit, exportedValue float64, exportUnit Unit) *MeterEnergyToday {
	return &MeterEnergyToday{
		importedValue: importedValue,
		importUnit:    importUnit,
		exportedValue: exportedValue,
		exportUnit:    exportUnit,
	}
}

func NewInverterEnergyToday(value float64, unit Unit) *InverterEnergyToday {
	return &InverterEnergyToday{
		producedValue: value,
		sourceUnit:    unit,
	}
}

func NewMeterActivePower(power float64, unit Unit) *MeterActivePower {
	return &MeterActivePower{
		power:      power,
		sourceUnit: unit,
	}
}

type InverterEnergyToday struct {
	producedValue float64
	sourceUnit    Unit
}

func (i *InverterEnergyToday) Produced() KWh {
	return i.sourceUnit.ToKWh(i.producedValue)
}

func (i *InverterActivePower) Producing() W {
	return i.sourceUnit.ToW(i.producingValue)
}

type MeterActivePower struct {
	power      float64
	sourceUnit Unit
}

func (m *MeterActivePower) Exporting() W {
	if m.power > 0 {
		return m.sourceUnit.ToW(m.power)
	}
	return m.sourceUnit.ToW(0.0)
}

func (m *MeterActivePower) Importing() W {
	if m.power < 0 {
		return m.sourceUnit.ToW(abs(m.power))
	}
	return m.sourceUnit.ToW(0.0)
}

type MeterEnergyToday struct {
	importedValue float64
	importUnit    Unit
	exportedValue float64
	exportUnit    Unit
}

func (m *MeterEnergyToday) Imported() KWh {
	return m.importUnit.ToKWh(m.importedValue)
}

func (m *MeterEnergyToday) Exported() KWh {
	return m.exportUnit.ToKWh(m.exportedValue)
}

func abs(v float64) float64 {
	if v < 0 {
		return -v
	}
	return v
}
