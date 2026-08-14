package kpis

import (
	"testing"
)

func newKpiLiveData(producing, meterPower float64) *KpiLiveData {
	wUnit := ParseUnitFromString("W")
	return &KpiLiveData{
		InverterActivePower: NewInverterActivePower(producing, wUnit),
		MeterActivePower:    NewMeterActivePower(meterPower, wUnit),
	}
}

func newKpiTodayData(produced, imported, exported float64) *KpiTodayData {
	kwhUnit := ParseUnitFromString("kWh")
	return &KpiTodayData{
		InverterTodayEnergy: NewInverterEnergyToday(produced, kwhUnit),
		MeterTodayEnergy:    NewMeterEnergyToday(imported, kwhUnit, exported, kwhUnit),
	}
}

func TestKpiLiveData_Consuming_DirectLoad(t *testing.T) {
	// With a LoadPower source (dongle register 37500), Consuming is the
	// measured value — the formula is not consulted (here it would give 2000).
	kpi := newKpiLiveData(5000, 3000)
	kpi.LoadPower = NewLoadPower(2100, ParseUnitFromString("W"))
	got := kpi.Consuming()
	if got.W != 2100 {
		t.Errorf("Consuming() = %v, want 2100", got.W)
	}
}

func TestKpiLiveData_Consuming(t *testing.T) {
	// Producing 5000W, exporting 3000W (positive meter = export)
	// Consuming = Producing - Exporting + Importing = 5000 - 3000 + 0 = 2000
	kpi := newKpiLiveData(5000, 3000)
	got := kpi.Consuming()
	if got.W != 2000 {
		t.Errorf("Consuming() = %v, want 2000", got.W)
	}
}

func TestKpiLiveData_Consuming_WithImport(t *testing.T) {
	// Producing 2000W, importing 1000W (negative meter = import)
	// Consuming = 2000 - 0 + 1000 = 3000
	kpi := newKpiLiveData(2000, -1000)
	got := kpi.Consuming()
	if got.W != 3000 {
		t.Errorf("Consuming() = %v, want 3000", got.W)
	}
}

func TestKpiLiveData_SelfConsuming(t *testing.T) {
	// Producing 5000W, exporting 3000W
	// SelfConsuming = 5000 - 3000 = 2000
	kpi := newKpiLiveData(5000, 3000)
	got := kpi.SelfConsuming()
	if got.W != 2000 {
		t.Errorf("SelfConsuming() = %v, want 2000", got.W)
	}
}

func TestKpiLiveData_SelfConsuming_NoExport(t *testing.T) {
	// Producing 2000W, importing 500W (negative meter)
	// SelfConsuming = 2000 - 0 = 2000
	kpi := newKpiLiveData(2000, -500)
	got := kpi.SelfConsuming()
	if got.W != 2000 {
		t.Errorf("SelfConsuming() = %v, want 2000", got.W)
	}
}

func TestKpiTodayData_Consumed(t *testing.T) {
	// Produced 20kWh, imported 5kWh, exported 8kWh
	// Consumed = 20 - 8 + 5 = 17
	kpi := newKpiTodayData(20, 5, 8)
	got := kpi.Consumed()
	if got.Kwh != 17 {
		t.Errorf("Consumed() = %v, want 17", got.Kwh)
	}
}

func TestKpiTodayData_SelfConsumed(t *testing.T) {
	// Produced 20kWh, exported 8kWh
	// SelfConsumed = 20 - 8 = 12
	kpi := newKpiTodayData(20, 5, 8)
	got := kpi.SelfConsumed()
	if got.Kwh != 12 {
		t.Errorf("SelfConsumed() = %v, want 12", got.Kwh)
	}
}

func TestKpiLiveData_ZeroProduction(t *testing.T) {
	// No production, importing 1000W
	// Consuming = 0 - 0 + 1000 = 1000
	kpi := newKpiLiveData(0, -1000)
	got := kpi.Consuming()
	if got.W != 1000 {
		t.Errorf("Consuming() = %v, want 1000", got.W)
	}
}

func TestKpiTodayData_ZeroProduction(t *testing.T) {
	// No production, imported 5kWh
	// Consumed = 0 - 0 + 5 = 5
	kpi := newKpiTodayData(0, 5, 0)
	got := kpi.Consumed()
	if got.Kwh != 5 {
		t.Errorf("Consumed() = %v, want 5", got.Kwh)
	}
}
