package collector

import (
	"fmt"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/request"
	"pvlogger-shared-modbus/response"
	"time"
)

func (s *ReadRegisterValuesService) ExecuteFetchDeviceIdentifiersJob() error {
	logging.Info("executing fetch device identifiers job", "job_id", s.jobId.Id)
	for _, deviceId := range sharedconfig.DevIdConfig().InverterDeviceIds {
		resp, err := s.readDeviceIdentifiers(deviceId)
		if err != nil {
			return err
		}
		if err := s.persistenceService.StoreValues(resp, sharedconfig.StoreDeviceIdValuesPath); err != nil {
			return err
		}
	}
	return nil
}

func (s *ReadRegisterValuesService) readDeviceIdentifiers(deviceId model.UByte) (*response.ReadDeviceIdentifiersResponse, error) {
	req := s.newReadDeviceIdentifiersRequest(deviceId)
	logging.Info("fetching device identifiers")
	if err := s.Connect(); err != nil {
		return nil, err
	}
	defer s.Close()
	mbapHeader, pduBytes, err := s.tcpHandler.ExecuteRequest(req.EncodeToBytes())
	s.Close()
	if err != nil {
		return nil, fmt.Errorf("error executing read device identifiers request: %w", err)
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
