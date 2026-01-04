package jobs

import (
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/response"
	"pvlogger-modbus-data-collector-go/internal/services/masterdata"
)

func (s *ReadRegisterValuesService) ExecuteFetchRegisterValuesJob() {
	logging.Info("executing fetch register values job", "job_id", s.jobId.Id)
	for deviceId, deviceTypes := range s.deviceIdToTypesMap {
		s.fetchRegisterValues(deviceId, deviceTypes)
	}
}

func (s *ReadRegisterValuesService) fetchRegisterValues(deviceId model.UByte, deviceTypes []model.DeviceType) {
	logging.Info("fetching register values", "device_id", deviceId, "device_types", deviceTypes)
	registerDefinitionsList := s.listsOfConsecutiveRegisters(deviceTypes)
	for _, registerDefinitions := range *registerDefinitionsList {
		logging.Info("fetching register values")
		req := s.NewReadRegisterValuesRequest(deviceId, registerDefinitions)
		s.Connect()
		mbapHeader, pduBytes, err := s.tcpHandler.ExecuteRequest(req.EncodeToBytes())
		s.Close()
		if err != nil {
			logging.Fatal("error requesting register values", "error", err)
		}
		valueResponse := response.NewRegisterValueResponseFromBytes(req.RegisterDefinitions, req.JobId, req.RequestId, req.RequestTime, *mbapHeader, pduBytes)
		s.persistenceService.StoreValues(valueResponse)
	}
	defer s.Close()
}

func (s *ReadRegisterValuesService) listsOfConsecutiveRegisters(deviceTypes []model.DeviceType) *[][]model.RegisterDefinition {
	registerDefinitions := s.fetchRegisterDefinitionsFromMasterData(deviceTypes)
	var listsOfConsecutiveRegisters [][]model.RegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []model.RegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	logging.Info("lists of consecutive registers", "count", len(listsOfConsecutiveRegisters))
	return &listsOfConsecutiveRegisters
}

func (s *ReadRegisterValuesService) fetchRegisterDefinitionsFromMasterData(deviceTypes []model.DeviceType) *[]model.RegisterDefinition {
	logging.Info("getting register definitions", "device_types", deviceTypes)
	definitions := masterdata.NewMasterDataService().FetchRegisterDefinitions(deviceTypes, s.categories)
	return definitions
}

func isNotSuccessor(registers [][]model.RegisterDefinition, nextRegister model.RegisterDefinition) bool {
	if len(registers) == 0 {
		return true
	}
	lastRegister := registers[len(registers)-1][len(registers[len(registers)-1])-1]
	lastRegisterAddress := lastRegister.RegisterAddress.Value
	nextRegisterAddress := lastRegisterAddress + lastRegister.Quantity.Value
	return nextRegister.RegisterAddress.Value != nextRegisterAddress
}
