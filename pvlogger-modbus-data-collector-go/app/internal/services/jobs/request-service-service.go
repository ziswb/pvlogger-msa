package jobs

import (
	"encoding/json"
	"fmt"
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/response"
	"pvlogger-modbus-data-collector-go/internal/services/masterdata"
)

func (s *RequestService) ExecuteFetchRegisterValuesJob() {
	log.Default().Println(fmt.Sprintf("Executing FetchDynamicRegisterValuesJob for Job ID '%s'", s.jobId.Id))
	for deviceId, deviceTypes := range s.deviceIdToTypesMap {
		s.fetchRegisterValues(deviceId, deviceTypes)
	}
}

func (s *RequestService) fetchRegisterValues(deviceId model.UByte, deviceTypes []model.DeviceType) {
	log.Default().Println("Fetching register values for Device ID:", deviceId, "having Device Types:", deviceTypes)
	registerDefinitionsList, err := s.listsOfConsecutiveRegisters(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions:", err)
		return
	}
	defer s.Close()
	for _, registerDefinitions := range *registerDefinitionsList {
		valueRequest := s.newRegisterValueRequest(deviceId, registerDefinitions)
		s.Connect()
		valueResponse, err := s.tcpHandler.FetchRegisterValues(valueRequest)
		s.Close()
		if err != nil {
			log.Default().Println("Error requesting register values:", err)
			return
		}
		err = s.storeResponse(valueResponse)
		if err != nil {
			log.Default().Println("Error storing response:", err)
			return
		}
	}
}

func (s *RequestService) listsOfConsecutiveRegisters(deviceTypes []model.DeviceType) (*[][]model.RegisterDefinition, error) {
	registerDefinitions, err := s.fetchRegisterDefinitionsFromMasterData(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions:", err)
		return &[][]model.RegisterDefinition{}, nil
	}
	var listsOfConsecutiveRegisters [][]model.RegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []model.RegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	log.Default().Println("Lists of consecutive registers:", len(listsOfConsecutiveRegisters))
	return &listsOfConsecutiveRegisters, nil
}

func (s *RequestService) fetchRegisterDefinitionsFromMasterData(deviceTypes []model.DeviceType) (*[]model.RegisterDefinition, error) {
	log.Default().Println("Getting register definitions for DeviceTypes", deviceTypes, "...")
	definitions, err := masterdata.NewMasterDataService().FetchRegisterDefinitions(deviceTypes, s.categories)
	if err != nil {
		return &[]model.RegisterDefinition{}, err
	}
	log.Default().Printf("Got register definitions")
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
	log.Default().Println("Response:", response)
	jsonResponse, err := json.MarshalIndent(response, "", "  ")
	if err != nil {
		return err
	}
	log.Default().Println("JSON Response:", string(jsonResponse))
	err = s.persistenceService.StoreValues(response)
	if err != nil {
		return err
	}
	return nil
}
