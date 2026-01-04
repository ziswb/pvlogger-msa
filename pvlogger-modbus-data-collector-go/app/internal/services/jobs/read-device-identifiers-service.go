package jobs

import (
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/request"
	"pvlogger-modbus-data-collector-go/internal/modbus/response"
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
	logging.Debug("fetching device identifiers")
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
	}
}
