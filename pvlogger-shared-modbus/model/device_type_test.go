package model

import (
	"testing"
)

func TestDeviceType_String(t *testing.T) {
	tests := []struct {
		name string
		dt   DeviceType
		want string
	}{
		{"INVERTER", InverterDeviceType, "INVERTER"},
		{"METER", MeterDeviceType, "METER"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.dt.String()
			if got != tt.want {
				t.Errorf("String() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestDeviceType_Int(t *testing.T) {
	tests := []struct {
		name string
		dt   DeviceType
		want int
	}{
		{"INVERTER is 1", InverterDeviceType, 1},
		{"METER is 47", MeterDeviceType, 47},
		{"unknown is -1", DeviceType("UNKNOWN"), -1},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.dt.Int()
			if got != tt.want {
				t.Errorf("Int() = %d, want %d", got, tt.want)
			}
		})
	}
}
