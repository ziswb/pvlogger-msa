package kpi

import (
	"math"
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	_ = os.Setenv("TZ", "Europe/Zurich")
	os.Exit(m.Run())
}

func TestMapToExportRecord_PowerConversion(t *testing.T) {
	kpis := &Values{
		CreatedAt:           1709208000000, // 2024-02-29 in millis
		SolarPower:          5.5,
		SolarToGridPower:    2.0,
		GridToHousePower:    1.5,
		DailyEnergyYield:    10.0,
		DailyEnergyExported: 4.0,
		DailyEnergyImported: 3.0,
		DailyEnergyConsumed: 9.0,
	}

	record := MapToExportRecord(kpis)

	if record.CurrentData.ProdAktW != 5500 {
		t.Errorf("ProdAktW: expected 5500, got %f", record.CurrentData.ProdAktW)
	}
	if record.CurrentData.VerkAktW != 2000 {
		t.Errorf("VerkAktW: expected 2000, got %f", record.CurrentData.VerkAktW)
	}
	if record.CurrentData.KaufAktW != 1500 {
		t.Errorf("KaufAktW: expected 1500, got %f", record.CurrentData.KaufAktW)
	}
}

func TestMapToExportRecord_ConsumptionIsMeasuredConsumption(t *testing.T) {
	kpis := &Values{
		SolarPower:        4.0,
		SolarToGridPower:  0.0, // nothing exported ...
		GridToHousePower:  0.0, // ... nothing imported: the house runs on solar
		SolarToHousePower: 2.5,
		OwnConsumption:    2.5,
	}

	record := MapToExportRecord(kpis)

	// Was export + import (= 0 W here); now the measured consumption.
	expected := 2500.0
	if record.CurrentData.VerbAktW != expected {
		t.Errorf("VerbAktW: expected %f, got %f", expected, record.CurrentData.VerbAktW)
	}
}

func TestMapToExportRecord_DailyEnergyConversion(t *testing.T) {
	kpis := &Values{
		DailyEnergyYield:    12.5,
		DailyEnergyExported: 6.3,
		DailyEnergyImported: 2.1,
		DailyEnergyConsumed: 8.3,
	}

	record := MapToExportRecord(kpis)

	if record.CurrentDayData.ProdTagWh != 12500 {
		t.Errorf("ProdTagWh: expected 12500, got %f", record.CurrentDayData.ProdTagWh)
	}
	if record.CurrentDayData.VerkTagWh != 6300 {
		t.Errorf("VerkTagWh: expected 6300, got %f", record.CurrentDayData.VerkTagWh)
	}
	if record.CurrentDayData.KaufTagWh != 2100 {
		t.Errorf("KaufTagWh: expected 2100, got %f", record.CurrentDayData.KaufTagWh)
	}
	if record.CurrentDayData.VerbTagWh != 8300 {
		t.Errorf("VerbTagWh: expected 8300, got %f", record.CurrentDayData.VerbTagWh)
	}
}

func TestMapToExportRecord_ZeroValues(t *testing.T) {
	kpis := &Values{}

	record := MapToExportRecord(kpis)

	if record.CurrentData.ProdAktW != 0 {
		t.Errorf("ProdAktW: expected 0, got %f", record.CurrentData.ProdAktW)
	}
	if record.CurrentData.VerkAktW != 0 {
		t.Errorf("VerkAktW: expected 0, got %f", record.CurrentData.VerkAktW)
	}
	if record.CurrentData.KaufAktW != 0 {
		t.Errorf("KaufAktW: expected 0, got %f", record.CurrentData.KaufAktW)
	}
	if record.CurrentData.VerbAktW != 0 {
		t.Errorf("VerbAktW: expected 0, got %f", record.CurrentData.VerbAktW)
	}
	if record.CurrentDayData.ProdTagWh != 0 {
		t.Errorf("ProdTagWh: expected 0, got %f", record.CurrentDayData.ProdTagWh)
	}
}

func TestMapToExportRecord_FractionalValues(t *testing.T) {
	kpis := &Values{
		SolarPower:       0.123,
		SolarToGridPower: 0.456,
		GridToHousePower: 0.789,
		OwnConsumption:   0.321,
	}

	record := MapToExportRecord(kpis)

	if math.Abs(record.CurrentData.ProdAktW-123.0) > 0.001 {
		t.Errorf("ProdAktW: expected 123.0, got %f", record.CurrentData.ProdAktW)
	}
	if math.Abs(record.CurrentData.VerkAktW-456.0) > 0.001 {
		t.Errorf("VerkAktW: expected 456.0, got %f", record.CurrentData.VerkAktW)
	}
	if math.Abs(record.CurrentData.KaufAktW-789.0) > 0.001 {
		t.Errorf("KaufAktW: expected 789.0, got %f", record.CurrentData.KaufAktW)
	}
	expectedVerb := 321.0
	if math.Abs(record.CurrentData.VerbAktW-expectedVerb) > 0.001 {
		t.Errorf("VerbAktW: expected %f, got %f", expectedVerb, record.CurrentData.VerbAktW)
	}
}

func TestMapToExportRecord_HasTimestamp(t *testing.T) {
	kpis := &Values{
		CreatedAt: 1709208000000,
	}

	record := MapToExportRecord(kpis)

	if record.LastUpdateTime == "" {
		t.Error("LastUpdateTime should not be empty")
	}
}
