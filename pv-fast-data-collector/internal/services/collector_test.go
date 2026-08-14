package services

import (
	"encoding/hex"
	"math"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/registers"
	"pvlogger-shared-modbus/response"
	"testing"
	"time"
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
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 37516}}, Value: int32(5000)},
			},
			targetDef: registers.DongleTotalActivePower,
			want:      5.0, // 5000 / 1000
		},
		{
			name: "no matching register returns 0",
			values: []*response.RegisterValue{
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 99999}}, Value: uint32(100)},
			},
			targetDef: registers.DongleTotalActivePower,
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
				{Definition: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 37502}}, Value: int32(-250)},
			},
			targetDef: registers.DongleGridPower,
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

// TestApplyDongleBlock_RealDevicePayload decodes a PDU captured live from the
// SmartDongle (unit 100, FC03 37498 x20) on 2026-08-14 10:23 while exporting:
// input=6974W load=885W grid=-6089W battery=0W total_active=6974W. It pins the
// whole chain: block tiling (incl. the 10-register MLD filler), I32 decoding,
// gain handling, and the grid sign flip to the meter convention. (The live-KPI
// mapping itself lives in the shared aggregate package since ADR-0016.)
func TestApplyDongleBlock_RealDevicePayload(t *testing.T) {
	data, err := hex.DecodeString("00001b3e00000375ffffe837000000000000000000000000000000000000000000572fd400001b3e")
	if err != nil {
		t.Fatalf("bad test payload: %v", err)
	}
	pdu := append([]byte{0x03, 0x28}, data...) // FC03, 0x28 = 40 payload bytes
	resp := response.NewRegisterValueResponseFromBytes(
		registers.DongleBlockRegisters(), model.NewJobId("test-job"), model.NewRequestId(1), time.Now(), model.MbapHeader{}, pdu)

	raw := &collectedRawValues{}
	applyDongleBlock(raw, resp.Values)

	assertFloat(t, "InverterActivePower (kW)", raw.InverterActivePower, 6.974)
	assertFloat(t, "MeterActivePower (W, positive=export)", raw.MeterActivePower, 6089)
	assertFloat(t, "LoadPower (W)", raw.LoadPower, 885)
	assertFloat(t, "BatteryPower (W)", raw.BatteryPower, 0)
}

// TestWithoutReservedBlock ensures the undocumented 37506–37515 filler never
// reaches the data accessor's generic register store.
func TestWithoutReservedBlock(t *testing.T) {
	data, err := hex.DecodeString("00001b3e00000375ffffe837000000000000000000000000000000000000000000572fd400001b3e")
	if err != nil {
		t.Fatalf("bad test payload: %v", err)
	}
	pdu := append([]byte{0x03, 0x28}, data...)
	resp := response.NewRegisterValueResponseFromBytes(
		registers.DongleBlockRegisters(), model.NewJobId("test-job"), model.NewRequestId(1), time.Now(), model.MbapHeader{}, pdu)

	filtered := withoutReservedBlock(resp.Values)
	if len(filtered) != len(registers.DongleBlockRegisters())-1 {
		t.Fatalf("filtered length = %d, want %d", len(filtered), len(registers.DongleBlockRegisters())-1)
	}
	for _, v := range filtered {
		if v.Definition.RegisterAddress.Value == registers.DongleReservedBlock.RegisterAddress.Value {
			t.Errorf("reserved block (register %d) survived the filter", v.Definition.RegisterAddress.Value)
		}
	}
}

func assertFloat(t *testing.T, name string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 0.001 {
		t.Errorf("%s = %f, want %f", name, got, want)
	}
}
