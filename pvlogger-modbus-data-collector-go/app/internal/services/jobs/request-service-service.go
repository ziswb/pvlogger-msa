package jobs

import (
	"encoding/json"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/response"
	"pvlogger-modbus-data-collector-go/internal/services/masterdata"
)

func (s *RequestService) ExecuteFetchRegisterValuesJob() {
	logging.Info("executing fetch register values job", "job_id", s.jobId.Id)
	for deviceId, deviceTypes := range s.deviceIdToTypesMap {
		s.fetchRegisterValues(deviceId, deviceTypes)
	}
}

func (s *RequestService) fetchRegisterValues(deviceId model.UByte, deviceTypes []model.DeviceType) {
	logging.Info("fetching register values", "device_id", deviceId, "device_types", deviceTypes)
	registerDefinitionsList, err := s.listsOfConsecutiveRegisters(deviceTypes)
	if err != nil {
		logging.Fatal("error getting register definitions", "error", err)
	}
	defer s.Close()
	for _, registerDefinitions := range *registerDefinitionsList {
		valueRequest := s.newRegisterValueRequest(deviceId, registerDefinitions)
		s.Connect()
		valueResponse, err := s.tcpHandler.FetchRegisterValues(valueRequest)
		s.Close()
		if err != nil {
			logging.Fatal("error requesting register values", "error", err)
		}
		err = s.storeResponse(valueResponse)
		if err != nil {
			logging.Fatal("error storing response", "error", err)
		}
	}
}

func (s *RequestService) listsOfConsecutiveRegisters(deviceTypes []model.DeviceType) (*[][]model.RegisterDefinition, error) {
	registerDefinitions, err := s.fetchRegisterDefinitionsFromMasterData(deviceTypes)
	if err != nil {
		logging.Fatal("error getting register definitions", "error", err)
	}
	var listsOfConsecutiveRegisters [][]model.RegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []model.RegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	logging.Debug("lists of consecutive registers", "count", len(listsOfConsecutiveRegisters))
	return &listsOfConsecutiveRegisters, nil
}

func (s *RequestService) fetchRegisterDefinitionsFromMasterData(deviceTypes []model.DeviceType) (*[]model.RegisterDefinition, error) {
	logging.Debug("getting register definitions", "device_types", deviceTypes)
	definitions, err := masterdata.NewMasterDataService().FetchRegisterDefinitions(deviceTypes, s.categories)
	if err != nil {
		logging.Fatal("error getting register definitions", "error", err)
	}
	logging.Debug("got register definitions")
	return definitions, nil
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

func (s *RequestService) storeResponse(response *response.RegisterValueResponse) error {
	logging.Debug("storing response", "response", response)
	jsonResponse, err := json.MarshalIndent(response, "", "  ")
	if err != nil {
		logging.Fatal("error marshalling response", "error", err)
	}
	logging.Debug("json response", "json", string(jsonResponse))
	err = s.persistenceService.StoreValues(response)
	if err != nil {
		logging.Fatal("error storing response", "error", err)
	}
	return nil
}
