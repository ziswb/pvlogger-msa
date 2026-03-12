package model

import (
	"testing"
)

func TestNewModbusRequestDataFromUShortValues(t *testing.T) {
	data := NewModbusRequestDataFromUShortValues(0x7530, 0x0002) // 30000, 2
	got := data.EncodeToBytes()

	if len(got) != 4 {
		t.Fatalf("expected 4 bytes, got %d", len(got))
	}
	// 0x7530 big-endian = [0x75, 0x30]
	if got[0] != 0x75 || got[1] != 0x30 {
		t.Errorf("first UShort = [%02x %02x], want [75 30]", got[0], got[1])
	}
	// 0x0002 big-endian = [0x00, 0x02]
	if got[2] != 0x00 || got[3] != 0x02 {
		t.Errorf("second UShort = [%02x %02x], want [00 02]", got[2], got[3])
	}
}

func TestNewModbusRequestDataFromUShortValues_Single(t *testing.T) {
	data := NewModbusRequestDataFromUShortValues(0xFFFF)
	got := data.EncodeToBytes()

	if len(got) != 2 {
		t.Fatalf("expected 2 bytes, got %d", len(got))
	}
	if got[0] != 0xFF || got[1] != 0xFF {
		t.Errorf("bytes = [%02x %02x], want [FF FF]", got[0], got[1])
	}
}

func TestNewModbusRequestDataFromUByteValues(t *testing.T) {
	data := NewModbusRequestDataFromUByteValues(0x0E, 0x01, 0x00)
	got := data.EncodeToBytes()

	if len(got) != 3 {
		t.Fatalf("expected 3 bytes, got %d", len(got))
	}
	if got[0] != 0x0E || got[1] != 0x01 || got[2] != 0x00 {
		t.Errorf("bytes = [%02x %02x %02x], want [0E 01 00]", got[0], got[1], got[2])
	}
}

func TestModbusRequestData_NumberOfBytes(t *testing.T) {
	tests := []struct {
		name string
		data ModbusRequestData
		want int
	}{
		{"two UShorts", NewModbusRequestDataFromUShortValues(1, 2), 4},
		{"three UBytes", NewModbusRequestDataFromUByteValues(1, 2, 3), 3},
		{"no values", NewModbusRequestDataFromUShortValues(), 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.data.NumberOfBytes()
			if got != tt.want {
				t.Errorf("NumberOfBytes() = %d, want %d", got, tt.want)
			}
		})
	}
}
