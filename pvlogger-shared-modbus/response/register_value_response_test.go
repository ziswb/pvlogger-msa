package response

import (
	"encoding/binary"
	"pvlogger-shared-modbus/model"
	"testing"
)

func regDef(name string, regType string, quantity int) model.RegisterDefinition {
	return model.RegisterDefinition{
		No:              1,
		Name:            name,
		ReadWrite:       "RO",
		Type:            regType,
		Unit:            "W",
		Gain:            "ONE",
		RegisterAddress: model.RegisterAddress{Value: 30000},
		Quantity:        model.Quantity{Value: quantity},
		Scope:           model.Scope{Value: "device"},
		Category:        "DYNAMIC",
	}
}

func u16Bytes(v uint16) []byte {
	b := make([]byte, 2)
	binary.BigEndian.PutUint16(b, v)
	return b
}

func u32Bytes(v uint32) []byte {
	b := make([]byte, 4)
	binary.BigEndian.PutUint32(b, v)
	return b
}

func TestHandleU16(t *testing.T) {
	payload := u16Bytes(42)
	rv := handleU16(regDef("test", "U16", 1), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(model.UShort) != 42 {
		t.Errorf("value = %v, want 42", rv.Value)
	}
}

func TestHandleU16_ShortPayload(t *testing.T) {
	rv := handleU16(regDef("test", "U16", 1), []byte{0x01})
	if rv != nil {
		t.Errorf("expected nil for short payload, got %v", rv)
	}
}

func TestHandleU32(t *testing.T) {
	payload := u32Bytes(100000)
	rv := handleU32(regDef("test", "U32", 2), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(model.UInt) != 100000 {
		t.Errorf("value = %v, want 100000", rv.Value)
	}
}

func TestHandleU32_ShortPayload(t *testing.T) {
	rv := handleU32(regDef("test", "U32", 2), []byte{0x01, 0x02})
	if rv != nil {
		t.Errorf("expected nil for short payload, got %v", rv)
	}
}

func TestHandleI16(t *testing.T) {
	// -123 as int16 in big-endian
	payload := u16Bytes(uint16(65536 - 123))
	rv := handleI16(regDef("test", "I16", 1), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(model.Short) != -123 {
		t.Errorf("value = %v, want -123", rv.Value)
	}
}

func TestHandleI16_ShortPayload(t *testing.T) {
	rv := handleI16(regDef("test", "I16", 1), []byte{0x01})
	if rv != nil {
		t.Errorf("expected nil for short payload, got %v", rv)
	}
}

func TestHandleI32(t *testing.T) {
	// -50000 as int32 in big-endian
	v := int32(-50000)
	payload := u32Bytes(uint32(v))
	rv := handleI32(regDef("test", "I32", 2), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(int32) != -50000 {
		t.Errorf("value = %v, want -50000", rv.Value)
	}
}

func TestHandleStr(t *testing.T) {
	payload := []byte("SUN2000")
	rv := handleStr(regDef("test", "STR", 4), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(string) != "SUN2000" {
		t.Errorf("value = %q, want %q", rv.Value, "SUN2000")
	}
}

func TestDecodeHuaweiModbusString_NullBytes(t *testing.T) {
	// "AB" followed by null bytes
	payload := []byte{0x41, 0x42, 0x00, 0x00, 0x00}
	got := decodeHuaweiModbusString(payload)
	if got != "AB" {
		t.Errorf("got %q, want %q", got, "AB")
	}
}

func TestDecodeHuaweiModbusString_AllNull(t *testing.T) {
	payload := []byte{0x00, 0x00, 0x00}
	got := decodeHuaweiModbusString(payload)
	if got != "" {
		t.Errorf("got %q, want empty string", got)
	}
}

func TestDecodeHuaweiModbusString_NullsBetweenWords(t *testing.T) {
	// "AB\x00\x00CD"
	payload := []byte{0x41, 0x42, 0x00, 0x00, 0x43, 0x44}
	got := decodeHuaweiModbusString(payload)
	if got != "AB CD" {
		t.Errorf("got %q, want %q", got, "AB CD")
	}
}

func TestDecodeHuaweiModbusString_Empty(t *testing.T) {
	got := decodeHuaweiModbusString([]byte{})
	if got != "" {
		t.Errorf("got %q, want empty string", got)
	}
}

func TestHandleBitField16(t *testing.T) {
	payload := u16Bytes(0x00FF)
	rv := handleBitField16(regDef("test", "BITFIELD_16", 1), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(model.UShort) != 0x00FF {
		t.Errorf("value = %v, want 255", rv.Value)
	}
}

func TestHandleBitField16_ShortPayload(t *testing.T) {
	rv := handleBitField16(regDef("test", "BITFIELD_16", 1), []byte{0x01})
	if rv != nil {
		t.Errorf("expected nil for short payload, got %v", rv)
	}
}

func TestHandleBitField32(t *testing.T) {
	payload := u32Bytes(0x0000FFFF)
	rv := handleBitField32(regDef("test", "BITFIELD_32", 2), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	if rv.Value.(model.UInt) != 0x0000FFFF {
		t.Errorf("value = %v, want 65535", rv.Value)
	}
}

func TestHandleBitField32_ShortPayload(t *testing.T) {
	rv := handleBitField32(regDef("test", "BITFIELD_32", 2), []byte{0x01, 0x02})
	if rv != nil {
		t.Errorf("expected nil for short payload, got %v", rv)
	}
}

func TestHandleMld(t *testing.T) {
	// Three int16 values: 1, -2, 3
	payload := make([]byte, 6)
	binary.BigEndian.PutUint16(payload[0:], uint16(1))
	v := int16(-2)
	binary.BigEndian.PutUint16(payload[2:], uint16(v))
	binary.BigEndian.PutUint16(payload[4:], uint16(3))

	rv := handleMld(regDef("test", "MLD", 3), payload)
	if rv == nil {
		t.Fatal("expected non-nil RegisterValue")
	}
	mld := rv.Value.([]model.Short)
	if len(mld) != 3 {
		t.Fatalf("mld length = %d, want 3", len(mld))
	}
	if mld[0] != 1 || mld[1] != -2 || mld[2] != 3 {
		t.Errorf("mld = %v, want [1, -2, 3]", mld)
	}
}

func TestHandleMld_OddPayload(t *testing.T) {
	rv := handleMld(regDef("test", "MLD", 1), []byte{0x01, 0x02, 0x03})
	if rv != nil {
		t.Errorf("expected nil for odd payload length, got %v", rv)
	}
}

func TestReadRegisterValue_UnknownType_FallsBackToU16(t *testing.T) {
	// Unknown type string maps to zero value of RegisterType (U16)
	// so it decodes as U16 rather than returning nil
	def := regDef("test", "UNKNOWN_TYPE", 1)
	rv := readRegisterValue(def, []byte{0x00, 0x01})
	if rv == nil {
		t.Fatal("expected non-nil (falls back to U16 via zero value)")
	}
	if rv.Value.(model.UShort) != 1 {
		t.Errorf("value = %v, want 1", rv.Value)
	}
}

func TestReadRegisterValues_MultipleRegisters(t *testing.T) {
	defs := []model.RegisterDefinition{
		regDef("power", "U16", 1),
		regDef("energy", "U32", 2),
	}
	// U16: 2 bytes, U32: 4 bytes
	payload := make([]byte, 6)
	binary.BigEndian.PutUint16(payload[0:], 500)
	binary.BigEndian.PutUint32(payload[2:], 100000)

	values := readRegisterValues(defs, payload)
	if len(values) != 2 {
		t.Fatalf("expected 2 values, got %d", len(values))
	}
	if values[0].Value.(model.UShort) != 500 {
		t.Errorf("first value = %v, want 500", values[0].Value)
	}
	if values[1].Value.(model.UInt) != 100000 {
		t.Errorf("second value = %v, want 100000", values[1].Value)
	}
}

func TestReadRegisterValues_PayloadUnderflow(t *testing.T) {
	defs := []model.RegisterDefinition{
		regDef("power", "U16", 1),
		regDef("energy", "U32", 2), // needs 4 more bytes, but only 2 available
	}
	payload := make([]byte, 4) // only enough for first register + partial second
	binary.BigEndian.PutUint16(payload[0:], 500)

	values := readRegisterValues(defs, payload)
	// Should decode first register, break on second
	if len(values) != 1 {
		t.Fatalf("expected 1 value (underflow on second), got %d", len(values))
	}
}

func TestCheckRegisterValuesPayload(t *testing.T) {
	defs := []model.RegisterDefinition{regDef("a", "U16", 1), regDef("b", "I32", 2)} // 6 bytes
	full := append([]byte{0x03, 0x06}, append(u16Bytes(1), u32Bytes(2)...)...)
	tests := []struct {
		name    string
		pdu     []byte
		wantErr bool
	}{
		{"complete", full, false},
		// The 2026-11-14 13:02 case: nothing but function code and one byte.
		{"exception-sized", []byte{0x83, 0x06}, true},
		{"empty", nil, true},
		{"truncated", full[:5], true},
		{"byte count mismatch", append([]byte{0x03, 0x04}, full[2:]...), true},
		{"wrong function code", append([]byte{0x04}, full[1:]...), true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := CheckRegisterValuesPayload(defs, tt.pdu)
			if (err != nil) != tt.wantErr {
				t.Errorf("CheckRegisterValuesPayload() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
