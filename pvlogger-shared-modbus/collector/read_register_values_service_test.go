package collector

import (
	"pvlogger-shared-modbus/model"
	"testing"
)

func TestIsNotSuccessor(t *testing.T) {
	tests := []struct {
		name      string
		registers [][]model.RegisterDefinition
		next      model.RegisterDefinition
		want      bool
	}{
		{
			name:      "empty slice",
			registers: nil,
			next:      model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30000}},
			want:      true,
		},
		{
			name: "direct successor with quantity 1",
			registers: [][]model.RegisterDefinition{
				{{RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 1}}},
			},
			next: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30001}},
			want: false,
		},
		{
			name: "direct successor with quantity 2",
			registers: [][]model.RegisterDefinition{
				{{RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 2}}},
			},
			next: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30002}},
			want: false,
		},
		{
			name: "gap between registers",
			registers: [][]model.RegisterDefinition{
				{{RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 1}}},
			},
			next: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30005}},
			want: true,
		},
		{
			name: "multiple groups uses last element of last group",
			registers: [][]model.RegisterDefinition{
				{{RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 1}}},
				{
					{RegisterAddress: model.RegisterAddress{Value: 30100}, Quantity: model.Quantity{Value: 1}},
					{RegisterAddress: model.RegisterAddress{Value: 30101}, Quantity: model.Quantity{Value: 2}},
				},
			},
			next: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30103}},
			want: false,
		},
		{
			name: "multiple groups non-successor",
			registers: [][]model.RegisterDefinition{
				{{RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 1}}},
				{{RegisterAddress: model.RegisterAddress{Value: 30100}, Quantity: model.Quantity{Value: 1}}},
			},
			next: model.RegisterDefinition{RegisterAddress: model.RegisterAddress{Value: 30200}},
			want: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := isNotSuccessor(tt.registers, tt.next)
			if got != tt.want {
				t.Errorf("isNotSuccessor() = %v, want %v", got, tt.want)
			}
		})
	}
}
