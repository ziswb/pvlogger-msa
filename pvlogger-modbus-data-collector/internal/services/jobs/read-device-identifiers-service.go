package jobs

import (
	"pvlogger-go-shared/logging"
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/request"
	"pvlogger-shared-modbus/response"
	"time"
)

func (s *ReadRegisterValuesService) ExecuteFetchDeviceIdentifiersJob() {
	logging.Info("executing fetch device identifiers job", "job_id", s.jobId.Id)
	for _, deviceId := range config.DevIdConfig().InverterDeviceIds {
		resp := s.readDeviceIdentifiers(deviceId)
		s.persistenceService.StoreDeviceIdentifierValues(resp)
	}
}

func (s *ReadRegisterValuesService) readDeviceIdentifiers(deviceId model.UByte) *response.ReadDeviceIdentifiersResponse {
	req := s.newReadDeviceIdentifiersRequest(deviceId)
	logging.Info("fetching device identifiers")
	s.Connect()
	defer s.Close()
	mbapHeader, pduBytes, err := s.tcpHandler.ExecuteRequest(req.EncodeToBytes())
	s.Close()
	if err != nil {
		logging.Fatal("error executing read device identifiers request", "error", err)
	}
	return response.NewReadDeviceIdentifiersResponseFromBytes(req.JobId, req.RequestId, req.RequestTime, *mbapHeader, pduBytes)
}

func (s *ReadRegisterValuesService) newReadDeviceIdentifiersRequest(deviceId model.UByte) *request.ReadDeviceIdentifiersRequest {
	return &request.ReadDeviceIdentifiersRequest{
		JobId:         s.jobId,
		RequestTime:   time.Now(),
		RequestId:     model.NewRequestId(s.NextRequestId()),
		LogicDeviceId: deviceId,
		ReadDevIdCode: s.ReadDevIdCode,
		ObjectId:      s.ObjectId,
	}
}
