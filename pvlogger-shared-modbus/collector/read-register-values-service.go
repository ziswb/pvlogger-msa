package collector

import (
	"fmt"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/masterdata"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/response"
)

func (s *ReadRegisterValuesService) ExecuteFetchRegisterValuesJob() error {
	logging.Info("executing fetch register values job", "job_id", s.jobId.Id)
	for deviceId, deviceTypes := range s.deviceIdToTypesMap {
		if err := s.fetchRegisterValues(deviceId, deviceTypes); err != nil {
			return err
		}
	}
	return nil
}

func (s *ReadRegisterValuesService) fetchRegisterValues(deviceId model.UByte, deviceTypes []model.DeviceType) error {
	logging.Info("fetching register values", "device_id", deviceId, "device_types", deviceTypes)
	registerDefinitionsList, err := s.listsOfConsecutiveRegisters(deviceTypes)
	if err != nil {
		return err
	}
	for _, registerDefinitions := range *registerDefinitionsList {
		logging.Info("fetching register values")
		req := s.NewReadRegisterValuesRequest(deviceId, registerDefinitions)
		if err := s.Connect(); err != nil {
			return err
		}
		mbapHeader, pduBytes, err := s.tcpHandler.ExecuteRequest(req.EncodeToBytes())
		s.Close()
		if err != nil {
			return fmt.Errorf("error requesting register values: %w", err)
		}
		if err := response.CheckRegisterValuesPayload(registerDefinitions, pduBytes); err != nil {
			return fmt.Errorf("not storing register values: %w", err)
		}
		valueResponse := response.NewRegisterValueResponseFromBytes(req.RegisterDefinitions, req.JobId, req.RequestId, req.RequestTime, *mbapHeader, pduBytes)
		if logging.DebugEnabled() {
			logging.Debug("register values received", "count", len(valueResponse.Values), "device_id", deviceId, "device_types", deviceTypes)
			for _, v := range valueResponse.Values {
				logging.Debug("register value", "register_address", v.Definition.RegisterAddress, "value", v.Value)
			}
		}
		if err := s.persistenceService.StoreValues(valueResponse, sharedconfig.StoreModbusValuesPath); err != nil {
			return err
		}
	}
	return nil
}

func (s *ReadRegisterValuesService) listsOfConsecutiveRegisters(deviceTypes []model.DeviceType) (*[][]model.RegisterDefinition, error) {
	registerDefinitions, err := s.fetchRegisterDefinitionsFromMasterData(deviceTypes)
	if err != nil {
		return nil, err
	}
	var listsOfConsecutiveRegisters [][]model.RegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []model.RegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	logging.Info("lists of consecutive registers", "count", len(listsOfConsecutiveRegisters))
	return &listsOfConsecutiveRegisters, nil
}

func (s *ReadRegisterValuesService) fetchRegisterDefinitionsFromMasterData(deviceTypes []model.DeviceType) (*[]model.RegisterDefinition, error) {
	logging.Info("getting register definitions", "device_types", deviceTypes)
	return masterdata.NewMasterDataService().FetchRegisterDefinitions(deviceTypes, s.categories)
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
