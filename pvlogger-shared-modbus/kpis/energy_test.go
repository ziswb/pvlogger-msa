package kpis

import (
	"testing"
)

func TestW_ToW(t *testing.T) {
	w := NewW(500)
	got := w.ToW()
	if got.W != 500 {
		t.Errorf("ToW() = %v, want 500", got.W)
	}
}

func TestW_ToKW(t *testing.T) {
	tests := []struct {
		name string
		w    float64
		want float64
	}{
		{"1000W to 1kW", 1000, 1.0},
		{"500W to 0.5kW", 500, 0.5},
		{"0W to 0kW", 0, 0.0},
		{"negative", -3000, -3.0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewW(tt.w).ToKW()
			if got.Kw != tt.want {
				t.Errorf("ToKW() = %v, want %v", got.Kw, tt.want)
			}
		})
	}
}

func TestKW_ToW(t *testing.T) {
	tests := []struct {
		name string
		kw   float64
		want float64
	}{
		{"1kW to 1000W", 1, 1000},
		{"0.5kW to 500W", 0.5, 500},
		{"0kW to 0W", 0, 0},
		{"negative", -2.5, -2500},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewKW(tt.kw).ToW()
			if got.W != tt.want {
				t.Errorf("ToW() = %v, want %v", got.W, tt.want)
			}
		})
	}
}

func TestKW_ToKW(t *testing.T) {
	kw := NewKW(3.5)
	got := kw.ToKW()
	if got.Kw != 3.5 {
		t.Errorf("ToKW() = %v, want 3.5", got.Kw)
	}
}

func TestWh_ToWh(t *testing.T) {
	wh := NewWh(750)
	got := wh.ToWh()
	if got.Wh != 750 {
		t.Errorf("ToWh() = %v, want 750", got.Wh)
	}
}

func TestWh_ToKWh(t *testing.T) {
	tests := []struct {
		name string
		wh   float64
		want float64
	}{
		{"1000Wh to 1kWh", 1000, 1.0},
		{"500Wh to 0.5kWh", 500, 0.5},
		{"0Wh to 0kWh", 0, 0.0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewWh(tt.wh).ToKWh()
			if got.Kwh != tt.want {
				t.Errorf("ToKWh() = %v, want %v", got.Kwh, tt.want)
			}
		})
	}
}

func TestKWh_ToWh(t *testing.T) {
	tests := []struct {
		name string
		kwh  float64
		want float64
	}{
		{"1kWh to 1000Wh", 1, 1000},
		{"2.5kWh to 2500Wh", 2.5, 2500},
		{"0kWh to 0Wh", 0, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewKWh(tt.kwh).ToWh()
			if got.Wh != tt.want {
				t.Errorf("ToWh() = %v, want %v", got.Wh, tt.want)
			}
		})
	}
}

func TestKWh_ToKWh(t *testing.T) {
	kwh := NewKWh(5.5)
	got := kwh.ToKWh()
	if got.Kwh != 5.5 {
		t.Errorf("ToKWh() = %v, want 5.5", got.Kwh)
	}
}

func TestW_Plus(t *testing.T) {
	got := NewW(300).Plus(NewW(200))
	if got.W != 500 {
		t.Errorf("Plus() = %v, want 500", got.W)
	}
}

func TestW_Minus(t *testing.T) {
	got := NewW(500).Minus(NewW(200))
	if got.W != 300 {
		t.Errorf("Minus() = %v, want 300", got.W)
	}
}

func TestKWh_Plus(t *testing.T) {
	got := NewKWh(3.5).Plus(NewKWh(1.5))
	if got.Kwh != 5.0 {
		t.Errorf("Plus() = %v, want 5.0", got.Kwh)
	}
}

func TestKWh_Minus(t *testing.T) {
	got := NewKWh(5.0).Minus(NewKWh(2.0))
	if got.Kwh != 3.0 {
		t.Errorf("Minus() = %v, want 3.0", got.Kwh)
	}
}

func TestW_Roundtrip(t *testing.T) {
	original := NewW(1234)
	roundtrip := original.ToKW().ToW()
	if roundtrip.W != original.W {
		t.Errorf("W->KW->W roundtrip = %v, want %v", roundtrip.W, original.W)
	}
}

func TestKWh_Roundtrip(t *testing.T) {
	original := NewKWh(5.678)
	roundtrip := original.ToWh().ToKWh()
	if roundtrip.Kwh != original.Kwh {
		t.Errorf("KWh->Wh->KWh roundtrip = %v, want %v", roundtrip.Kwh, original.Kwh)
	}
}
