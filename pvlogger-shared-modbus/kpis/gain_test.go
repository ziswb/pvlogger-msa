package kpis

import (
	"testing"
)

func TestNewGainFromString(t *testing.T) {
	tests := []struct {
		name string
		text string
		want Gain
	}{
		{"ONE", "ONE", ONE},
		{"TEN", "TEN", TEN},
		{"HUNDRED", "HUNDRED", HUNDRED},
		{"THOUSAND", "THOUSAND", THOUSAND},
		{"ASTERISK", "ASTERISK", ASTERISK},
		{"unknown defaults to ASTERISK", "INVALID", ASTERISK},
		{"empty defaults to ASTERISK", "", ASTERISK},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewGainFromString(tt.text)
			if got != tt.want {
				t.Errorf("NewGainFromString(%q) = %v, want %v", tt.text, got, tt.want)
			}
		})
	}
}

func TestGain_String(t *testing.T) {
	tests := []struct {
		name string
		gain Gain
		want string
	}{
		{"ONE", ONE, "1"},
		{"TEN", TEN, "10"},
		{"HUNDRED", HUNDRED, "100"},
		{"THOUSAND", THOUSAND, "1000"},
		{"ASTERISK", ASTERISK, "1"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.gain.String()
			if got != tt.want {
				t.Errorf("Gain(%v).String() = %q, want %q", tt.gain, got, tt.want)
			}
		})
	}
}
