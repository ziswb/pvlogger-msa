package model

import (
	"testing"
)

func TestRegisterType_ToString(t *testing.T) {
	tests := []struct {
		name string
		rt   RegisterType
		want string
	}{
		{"U16", RegisterTypeU16, "U16"},
		{"U32", RegisterTypeU32, "U32"},
		{"I16", RegisterTypeI16, "I16"},
		{"I32", RegisterTypeI32, "I32"},
		{"STR", RegisterTypeSTR, "STR"},
		{"MLD", RegisterTypeMLD, "MLD"},
		{"BITFIELD_16", RegisterTypeBitfield16, "BITFIELD_16"},
		{"BITFIELD_32", RegisterTypeBitfield32, "BITFIELD_32"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.rt.ToString()
			if got != tt.want {
				t.Errorf("ToString() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestToRegisterType(t *testing.T) {
	tests := []struct {
		name string
		s    string
		want RegisterType
	}{
		{"U16", "U16", RegisterTypeU16},
		{"U32", "U32", RegisterTypeU32},
		{"I16", "I16", RegisterTypeI16},
		{"I32", "I32", RegisterTypeI32},
		{"STR", "STR", RegisterTypeSTR},
		{"MLD", "MLD", RegisterTypeMLD},
		{"BITFIELD_16", "BITFIELD_16", RegisterTypeBitfield16},
		{"BITFIELD_32", "BITFIELD_32", RegisterTypeBitfield32},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ToRegisterType(tt.s)
			if got != tt.want {
				t.Errorf("ToRegisterType(%q) = %v, want %v", tt.s, got, tt.want)
			}
		})
	}
}

func TestRegisterType_Roundtrip(t *testing.T) {
	types := []RegisterType{
		RegisterTypeU16, RegisterTypeU32, RegisterTypeI16, RegisterTypeI32,
		RegisterTypeSTR, RegisterTypeMLD, RegisterTypeBitfield16, RegisterTypeBitfield32,
	}
	for _, rt := range types {
		name := rt.ToString()
		t.Run(name, func(t *testing.T) {
			got := ToRegisterType(name)
			if got != rt {
				t.Errorf("roundtrip for %q: got %v, want %v", name, got, rt)
			}
		})
	}
}

func TestToRegisterType_Unknown(t *testing.T) {
	got := ToRegisterType("UNKNOWN")
	// Zero value of RegisterType is RegisterTypeU16 (iota 0)
	if got != RegisterTypeU16 {
		t.Errorf("ToRegisterType(UNKNOWN) = %v, want %v (zero value)", got, RegisterTypeU16)
	}
}
