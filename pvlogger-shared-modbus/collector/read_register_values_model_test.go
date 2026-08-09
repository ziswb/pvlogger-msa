package collector

import (
	"pvlogger-shared-modbus/model"
	"testing"
)

func TestNextRequestId_Increments(t *testing.T) {
	s := &ReadRegisterValuesService{}

	first := s.NextRequestId()
	second := s.NextRequestId()
	third := s.NextRequestId()

	if first != 1 {
		t.Errorf("expected first request id to be 1, got %d", first)
	}
	if second != 2 {
		t.Errorf("expected second request id to be 2, got %d", second)
	}
	if third != 3 {
		t.Errorf("expected third request id to be 3, got %d", third)
	}
}

func TestNewReadRegisterValuesRequest(t *testing.T) {
	s := &ReadRegisterValuesService{
		jobId: model.NewJobId("test-job-123"),
	}

	regDefs := []model.RegisterDefinition{
		{Name: "reg1", RegisterAddress: model.RegisterAddress{Value: 30000}, Quantity: model.Quantity{Value: 1}},
		{Name: "reg2", RegisterAddress: model.RegisterAddress{Value: 30001}, Quantity: model.Quantity{Value: 2}},
	}

	deviceId := model.UByte(1)
	req := s.NewReadRegisterValuesRequest(deviceId, regDefs)

	if req.JobId.Id != "test-job-123" {
		t.Errorf("expected job id 'test-job-123', got '%s'", req.JobId.Id)
	}
	if req.RequestId.Id != 1 {
		t.Errorf("expected request id 1, got %d", req.RequestId.Id)
	}
	if req.LogicDeviceId != deviceId {
		t.Errorf("expected device id %d, got %d", deviceId, req.LogicDeviceId)
	}
	if len(req.RegisterDefinitions) != 2 {
		t.Errorf("expected 2 register definitions, got %d", len(req.RegisterDefinitions))
	}
	if req.RequestTime.IsZero() {
		t.Error("expected request time to be set")
	}
}

func TestAppendIdToTypeEntries(t *testing.T) {
	tests := []struct {
		name       string
		initial    DeviceIdToTypesMap
		ids        []model.UByte
		deviceType model.DeviceType
		wantLen    int
		check      func(t *testing.T, m DeviceIdToTypesMap)
	}{
		{
			name:       "single device",
			initial:    make(DeviceIdToTypesMap),
			ids:        []model.UByte{1},
			deviceType: model.InverterDeviceType,
			wantLen:    1,
			check: func(t *testing.T, m DeviceIdToTypesMap) {
				if types := m[1]; len(types) != 1 || types[0] != model.InverterDeviceType {
					t.Errorf("device 1: want [INVERTER], got %v", types)
				}
			},
		},
		{
			name:       "multiple devices",
			initial:    make(DeviceIdToTypesMap),
			ids:        []model.UByte{1, 2, 3},
			deviceType: model.MeterDeviceType,
			wantLen:    3,
			check: func(t *testing.T, m DeviceIdToTypesMap) {
				for _, id := range []model.UByte{1, 2, 3} {
					if types := m[id]; len(types) != 1 || types[0] != model.MeterDeviceType {
						t.Errorf("device %d: want [METER], got %v", id, types)
					}
				}
			},
		},
		{
			name:       "empty ids",
			initial:    make(DeviceIdToTypesMap),
			ids:        []model.UByte{},
			deviceType: model.InverterDeviceType,
			wantLen:    0,
			check:      func(t *testing.T, m DeviceIdToTypesMap) {},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			appendIdToTypeEntries(tt.initial, tt.ids, tt.deviceType)
			if got := len(tt.initial); got != tt.wantLen {
				t.Fatalf("map length: want %d, got %d", tt.wantLen, got)
			}
			tt.check(t, tt.initial)
		})
	}
}

func TestAppendIdToTypeEntries_SameDeviceMultipleTypes(t *testing.T) {
	m := make(DeviceIdToTypesMap)

	appendIdToTypeEntries(m, []model.UByte{1}, model.InverterDeviceType)
	appendIdToTypeEntries(m, []model.UByte{1}, model.MeterDeviceType)

	if len(m) != 1 {
		t.Fatalf("want 1 entry, got %d", len(m))
	}
	types := m[1]
	if len(types) != 2 {
		t.Fatalf("want 2 types for device 1, got %d", len(types))
	}
	if types[0] != model.InverterDeviceType {
		t.Errorf("want first type INVERTER, got %s", types[0])
	}
	if types[1] != model.MeterDeviceType {
		t.Errorf("want second type METER, got %s", types[1])
	}
}
