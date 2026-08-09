package collector

import (
	"pvlogger-shared-modbus/model"
	"testing"
)

func TestNewReadDeviceIdentifiersRequest(t *testing.T) {
	tests := []struct {
		name          string
		readDevIdCode model.UByte
		objectId      model.UByte
	}{
		{
			name:          "device identifiers codes",
			readDevIdCode: 0x01,
			objectId:      0x00,
		},
		{
			name:          "device list codes",
			readDevIdCode: 0x03,
			objectId:      0x87,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &ReadRegisterValuesService{
				jobId:         model.NewJobId("test-job"),
				ReadDevIdCode: tt.readDevIdCode,
				ObjectId:      tt.objectId,
			}

			req := s.newReadDeviceIdentifiersRequest(model.UByte(5))

			if req.JobId.Id != "test-job" {
				t.Errorf("JobId = %q, want %q", req.JobId.Id, "test-job")
			}
			if req.LogicDeviceId != 5 {
				t.Errorf("LogicDeviceId = %d, want 5", req.LogicDeviceId)
			}
			if req.ReadDevIdCode != tt.readDevIdCode {
				t.Errorf("ReadDevIdCode = 0x%02x, want 0x%02x", req.ReadDevIdCode, tt.readDevIdCode)
			}
			if req.ObjectId != tt.objectId {
				t.Errorf("ObjectId = 0x%02x, want 0x%02x", req.ObjectId, tt.objectId)
			}
			if req.RequestTime.IsZero() {
				t.Error("RequestTime should be set")
			}
		})
	}
}

func TestNewReadDeviceIdentifiersRequest_IncrementsRequestId(t *testing.T) {
	s := &ReadRegisterValuesService{
		jobId:         model.NewJobId("test-job"),
		ReadDevIdCode: 0x01,
		ObjectId:      0x00,
	}

	req1 := s.newReadDeviceIdentifiersRequest(model.UByte(1))
	req2 := s.newReadDeviceIdentifiersRequest(model.UByte(2))

	if req1.RequestId.Id != 1 {
		t.Errorf("first RequestId = %d, want 1", req1.RequestId.Id)
	}
	if req2.RequestId.Id != 2 {
		t.Errorf("second RequestId = %d, want 2", req2.RequestId.Id)
	}
}
