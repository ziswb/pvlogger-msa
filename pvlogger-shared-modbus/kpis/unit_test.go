package kpis

import (
	"testing"
)

func TestParseUnitFromString(t *testing.T) {
	tests := []struct {
		name string
		text string
		want UnitId
	}{
		{"W", "W", UnitW},
		{"kW", "kW", UnitKW},
		{"Wh", "Wh", UnitWh},
		{"kWh", "kWh", UnitKWh},
		{"uppercase W", "W", UnitW},
		{"uppercase KW", "KW", UnitKW},
		{"uppercase WH", "WH", UnitWh},
		{"uppercase KWH", "KWH", UnitKWh},
		{"lowercase kw", "kw", UnitKW},
		{"lowercase kwh", "kwh", UnitKWh},
		{"unknown defaults to W", "xyz", UnitW},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ParseUnitFromString(tt.text)
			if got.unitId != tt.want {
				t.Errorf("ParseUnitFromString(%q) = %v, want %v", tt.text, got.unitId, tt.want)
			}
		})
	}
}

func TestUnit_ToW(t *testing.T) {
	tests := []struct {
		name  string
		unit  Unit
		value float64
		want  float64
	}{
		{"W stays W", ParseUnitFromString("W"), 500, 500},
		{"kW to W", ParseUnitFromString("kW"), 2.5, 2500},
		{"invalid unit returns 0", ParseUnitFromString("kWh"), 100, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.unit.ToW(tt.value)
			if got.W != tt.want {
				t.Errorf("ToW(%v) = %v, want %v", tt.value, got.W, tt.want)
			}
		})
	}
}

func TestUnit_ToKWh(t *testing.T) {
	tests := []struct {
		name  string
		unit  Unit
		value float64
		want  float64
	}{
		{"kWh stays kWh", ParseUnitFromString("kWh"), 5.5, 5.5},
		{"Wh to kWh", ParseUnitFromString("Wh"), 2500, 2.5},
		{"invalid unit returns 0", ParseUnitFromString("W"), 100, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.unit.ToKWh(tt.value)
			if got.Kwh != tt.want {
				t.Errorf("ToKWh(%v) = %v, want %v", tt.value, got.Kwh, tt.want)
			}
		})
	}
}
