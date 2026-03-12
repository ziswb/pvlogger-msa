package response

import (
	"testing"
)

func TestUByteArray_MarshalJSON(t *testing.T) {
	tests := []struct {
		name string
		arr  UByteArray
		want string
	}{
		{"nil", nil, "null"},
		{"empty", UByteArray{}, "[]"},
		{"single element", UByteArray{42}, "[42]"},
		{"multiple elements", UByteArray{0x41, 0x42, 0x43}, "[65,66,67]"},
		{"max byte value", UByteArray{255}, "[255]"},
		{"zero value", UByteArray{0}, "[0]"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := tt.arr.MarshalJSON()
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if string(got) != tt.want {
				t.Errorf("MarshalJSON() = %q, want %q", string(got), tt.want)
			}
		})
	}
}
