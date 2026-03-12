package services

import (
	"math"
	"pv-fast-data-collector/internal/registers"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/response"
	"testing"
)

func TestToFloat64(t *testing.T) {
	tests := []struct {
		name  string
		input any
		want  float64
	}{
		{"uint32", uint32(12345), 12345.0},
		{"int32 negative", int32(-500), -500.0},
		{"int32 zero", int32(0), 0.0},
		{"unsupported type returns 0", "not a number", 0.0},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := toFloat64(tt.input)
			if got != tt.want {
				t.Errorf("toFloat64(%v) = %f, want %f", tt.input, got, tt.want)
			}
		})
	}
}

func TestToFloat64FromGain(t *testing.T) {
	tests := []struct {
		name    string
		gainStr string
		want    float64
	}{
		{"THOUSAND", "THOUSAND", 1000.0},
		{"HUNDRED", "HUNDRED", 100.0},
		{"ONE", "ONE", 1.0},
		{"unknown falls back to ASTERISK (1)", "UNKNOWN", 1.0},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := toFloat64FromGain(tt.gainStr)
			if got != tt.want {
				t.Errorf("toFloat64FromGain(%q) = %f, want %f", tt.gainStr, got, tt.want)
			}
		})
	}
}

func TestApplyGain(t *testing.T) {
	tests := []struct {
		name      string
		values    []*response.RegisterValue
		targetDef model.RegisterDefinition
		want      float64
	}{
		{
			name: "matching register with THOUSAND gain",
			values: []*response.RegisterValue{
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 32080}}, Value: uint32(5000)},
			},
			targetDef: registers.InverterActivePower,
			want:      5.0, // 5000 / 1000
		},
		{
			name: "no matching register returns 0",
			values: []*response.RegisterValue{
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 99999}}, Value: uint32(100)},
			},
			targetDef: registers.InverterActivePower,
			want:      0.0,
		},
		{
			name: "selects correct register from multiple values",
			values: []*response.RegisterValue{
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 37119}}, Value: uint32(500)},
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 37121}}, Value: uint32(300)},
			},
			targetDef: registers.MeterExportedEnergy,
			want:      5.0, // 500 / 100
		},
		{
			name: "gain ONE with negative I32",
			values: []*response.RegisterValue{
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 37113}}, Value: int32(-250)},
			},
			targetDef: registers.MeterActivePower,
			want:      -250.0, // -250 / 1
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := applyGain(tt.values, tt.targetDef)
			if math.Abs(got-tt.want) > 0.001 {
				t.Errorf("applyGain() = %f, want %f", got, tt.want)
			}
		})
	}
}

func TestComputeLiveKpis(t *testing.T) {
	tests := []struct {
		name string
		raw  *collectedRawValues
		want struct {
			producing, exporting, importing, consuming, selfConsuming float64
		}
	}{
		{
			name: "producing and exporting",
			raw:  &collectedRawValues{InverterActivePower: 5.0, MeterActivePower: 2000.0},
			want: struct{ producing, exporting, importing, consuming, selfConsuming float64 }{
				producing: 5000, exporting: 2000, importing: 0, consuming: 3000, selfConsuming: 3000,
			},
		},
		{
			name: "producing and importing",
			raw:  &collectedRawValues{InverterActivePower: 1.0, MeterActivePower: -500.0},
			want: struct{ producing, exporting, importing, consuming, selfConsuming float64 }{
				producing: 1000, exporting: 0, importing: 500, consuming: 1500, selfConsuming: 1000,
			},
		},
		{
			name: "zero production with import",
			raw:  &collectedRawValues{InverterActivePower: 0.0, MeterActivePower: -1000.0},
			want: struct{ producing, exporting, importing, consuming, selfConsuming float64 }{
				producing: 0, exporting: 0, importing: 1000, consuming: 1000, selfConsuming: 0,
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			live := computeLiveKpis(tt.raw)

			assertFloat(t, "Producing", live.Producing().W, tt.want.producing)
			assertFloat(t, "Exporting", live.Exporting().W, tt.want.exporting)
			assertFloat(t, "Importing", live.Importing().W, tt.want.importing)
			assertFloat(t, "Consuming", live.Consuming().W, tt.want.consuming)
			assertFloat(t, "SelfConsuming", live.SelfConsuming().W, tt.want.selfConsuming)
		})
	}
}

func TestComputeTodayKpis(t *testing.T) {
	tests := []struct {
		name        string
		raw         *collectedRawValues
		meterEnergy *MeterEnergyTodayResponse
		want        struct {
			produced, exported, imported, consumed, selfConsumed float64
		}
	}{
		{
			name:        "typical day values",
			raw:         &collectedRawValues{InverterDailyYield: 15.5},
			meterEnergy: &MeterEnergyTodayResponse{ExportedEnergyToday: 8.0, ImportedEnergyToday: 2.0},
			want: struct{ produced, exported, imported, consumed, selfConsumed float64 }{
				produced: 15500, exported: 8000, imported: 2000, consumed: 9500, selfConsumed: 7500,
			},
		},
		{
			name:        "zero values",
			raw:         &collectedRawValues{},
			meterEnergy: &MeterEnergyTodayResponse{},
			want: struct{ produced, exported, imported, consumed, selfConsumed float64 }{
				produced: 0, exported: 0, imported: 0, consumed: 0, selfConsumed: 0,
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			today := computeTodayKpis(tt.raw, tt.meterEnergy)

			assertFloat(t, "Produced", today.Produced().ToWh().Wh, tt.want.produced)
			assertFloat(t, "Exported", today.Exported().ToWh().Wh, tt.want.exported)
			assertFloat(t, "Imported", today.Imported().ToWh().Wh, tt.want.imported)
			assertFloat(t, "Consumed", today.Consumed().ToWh().Wh, tt.want.consumed)
			assertFloat(t, "SelfConsumed", today.SelfConsumed().ToWh().Wh, tt.want.selfConsumed)
		})
	}
}

func assertFloat(t *testing.T, name string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 0.001 {
		t.Errorf("%s = %f, want %f", name, got, want)
	}
}
