package model

import (
	"testing"
)

func TestMbapHeader_EncodeToBytes(t *testing.T) {
	header := MbapHeader{
		ProtocolIdentifier: 0x0001,
		ProtocolType:       0x0000,
		DataLength:         0x0006,
		LogicDeviceId:      0x01,
	}
	got := header.EncodeToBytes()

	if len(got) != 7 {
		t.Fatalf("EncodeToBytes() length = %d, want 7", len(got))
	}
	// ProtocolIdentifier big-endian
	if got[0] != 0x00 || got[1] != 0x01 {
		t.Errorf("ProtocolIdentifier = [%02x %02x], want [00 01]", got[0], got[1])
	}
	// ProtocolType big-endian
	if got[2] != 0x00 || got[3] != 0x00 {
		t.Errorf("ProtocolType = [%02x %02x], want [00 00]", got[2], got[3])
	}
	// DataLength big-endian
	if got[4] != 0x00 || got[5] != 0x06 {
		t.Errorf("DataLength = [%02x %02x], want [00 06]", got[4], got[5])
	}
	// LogicDeviceId
	if got[6] != 0x01 {
		t.Errorf("LogicDeviceId = %02x, want 01", got[6])
	}
}

func TestMbapHeader_EncodeToBytes_LargeValues(t *testing.T) {
	header := MbapHeader{
		ProtocolIdentifier: 0xABCD,
		ProtocolType:       0x1234,
		DataLength:         0xFF00,
		LogicDeviceId:      0xFE,
	}
	got := header.EncodeToBytes()

	if got[0] != 0xAB || got[1] != 0xCD {
		t.Errorf("ProtocolIdentifier = [%02x %02x], want [AB CD]", got[0], got[1])
	}
	if got[2] != 0x12 || got[3] != 0x34 {
		t.Errorf("ProtocolType = [%02x %02x], want [12 34]", got[2], got[3])
	}
	if got[4] != 0xFF || got[5] != 0x00 {
		t.Errorf("DataLength = [%02x %02x], want [FF 00]", got[4], got[5])
	}
	if got[6] != 0xFE {
		t.Errorf("LogicDeviceId = %02x, want FE", got[6])
	}
}

func TestNewMbapHeaderFromBytes(t *testing.T) {
	input := []byte{0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x01}
	got := NewMbapHeaderFromBytes(input)

	if got.ProtocolIdentifier != 0x0001 {
		t.Errorf("ProtocolIdentifier = %d, want 1", got.ProtocolIdentifier)
	}
	if got.ProtocolType != 0x0000 {
		t.Errorf("ProtocolType = %d, want 0", got.ProtocolType)
	}
	if got.DataLength != 0x0006 {
		t.Errorf("DataLength = %d, want 6", got.DataLength)
	}
	if got.LogicDeviceId != 0x01 {
		t.Errorf("LogicDeviceId = %d, want 1", got.LogicDeviceId)
	}
}

func TestNewMbapHeaderFromBytes_InvalidLength(t *testing.T) {
	got := NewMbapHeaderFromBytes([]byte{0x00, 0x01, 0x02})
	// Should return empty header
	if got.ProtocolIdentifier != 0 || got.DataLength != 0 || got.LogicDeviceId != 0 {
		t.Errorf("expected empty header for invalid input, got %+v", got)
	}
}

func TestMbapHeader_Roundtrip(t *testing.T) {
	original := MbapHeader{
		ProtocolIdentifier: 42,
		ProtocolType:       0,
		DataLength:         100,
		LogicDeviceId:      5,
	}
	encoded := original.EncodeToBytes()
	decoded := NewMbapHeaderFromBytes(encoded)

	if decoded != original {
		t.Errorf("roundtrip failed: got %+v, want %+v", decoded, original)
	}
}
