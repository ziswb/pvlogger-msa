package kpis

import (
	"testing"
)

func TestInverterActivePower_Producing(t *testing.T) {
	tests := []struct {
		name  string
		value float64
		unit  string
		want  float64
	}{
		{"W value", 5000, "W", 5000},
		{"kW value", 3.5, "kW", 3500},
		{"zero", 0, "W", 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := NewInverterActivePower(tt.value, ParseUnitFromString(tt.unit))
			got := p.Producing()
			if got.W != tt.want {
				t.Errorf("Producing() = %v, want %v", got.W, tt.want)
			}
		})
	}
}

func TestMeterActivePower_Exporting(t *testing.T) {
	tests := []struct {
		name  string
		power float64
		want  float64
	}{
		{"positive power exports", 3000, 3000},
		{"zero power no export", 0, 0},
		{"negative power no export", -2000, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := NewMeterActivePower(tt.power, ParseUnitFromString("W"))
			got := m.Exporting()
			if got.W != tt.want {
				t.Errorf("Exporting() = %v, want %v", got.W, tt.want)
			}
		})
	}
}

func TestMeterActivePower_Importing(t *testing.T) {
	tests := []struct {
		name  string
		power float64
		want  float64
	}{
		{"negative power imports abs", -2000, 2000},
		{"zero power no import", 0, 0},
		{"positive power no import", 3000, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := NewMeterActivePower(tt.power, ParseUnitFromString("W"))
			got := m.Importing()
			if got.W != tt.want {
				t.Errorf("Importing() = %v, want %v", got.W, tt.want)
			}
		})
	}
}

func TestInverterEnergyToday_Produced(t *testing.T) {
	tests := []struct {
		name  string
		value float64
		unit  string
		want  float64
	}{
		{"kWh value", 15.5, "kWh", 15.5},
		{"Wh value", 5000, "Wh", 5.0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := NewInverterEnergyToday(tt.value, ParseUnitFromString(tt.unit))
			got := e.Produced()
			if got.Kwh != tt.want {
				t.Errorf("Produced() = %v, want %v", got.Kwh, tt.want)
			}
		})
	}
}

func TestMeterEnergyToday_Imported(t *testing.T) {
	e := NewMeterEnergyToday(3.5, ParseUnitFromString("kWh"), 7.0, ParseUnitFromString("kWh"))
	got := e.Imported()
	if got.Kwh != 3.5 {
		t.Errorf("Imported() = %v, want 3.5", got.Kwh)
	}
}

func TestMeterEnergyToday_Exported(t *testing.T) {
	e := NewMeterEnergyToday(3.5, ParseUnitFromString("kWh"), 7.0, ParseUnitFromString("kWh"))
	got := e.Exported()
	if got.Kwh != 7.0 {
		t.Errorf("Exported() = %v, want 7.0", got.Kwh)
	}
}
