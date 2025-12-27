package services

import (
	"encoding/json"
	"fmt"
	"log"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/modbus/request"
	"sync/atomic"
	"time"
)

const (
	MeterDeviceType    = "METER"
	InverterDeviceType = "INVERTER"
)

var (
	Categories        = []string{DynamicCategory, AlarmCategory}
	LastTransactionId = atomic.Int32{}
)

type DeviceIdToTypesMap map[model.UByte][]string

func NewDeviceIdToTypesMap() DeviceIdToTypesMap {
	var deviceIdToTypesMap = make(map[model.UByte][]string)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().MeterDeviceIds, MeterDeviceType)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().InverterDeviceIds, InverterDeviceType)
	return deviceIdToTypesMap
}

func updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap map[model.UByte][]string, devIds []model.UByte, deviceType string) {
	for _, id := range devIds {
		if len(deviceIdToTypesMap[id]) == 0 {
			deviceIdToTypesMap[id] = []string{deviceType}
		} else {
			deviceIdToTypesMap[id] = append(deviceIdToTypesMap[id], deviceType)
		}
	}
}

func fetchRegisterValues(jobId model.JobId, deviceId model.UByte, deviceTypes []string) {
	log.Default().Println("Fetching register values for Device ID:", deviceId, "having Device Types:", deviceTypes)
	registerDefinitionsList, err := listsOfConsecutiveRegisters(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions:", err)
		return
	}
	url := fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port)
	tcpHandler := modbus.NewTcpHandler(url, config.ModbusConfig().Timeout)
	defer func(tcpHandler *modbus.TcpHandler) {
		err := tcpHandler.Close()
		if err != nil {
			log.Default().Println("Error closing Modbus connection:", err)
		}
	}(tcpHandler)
	err = tcpHandler.Connect()
	if err != nil {
		log.Default().Println("Error connecting to Modbus device:", err)
		return
	}
	for _, registerDefinitions := range *registerDefinitionsList {
		request := request.ReadHoldingRegistersValueRequest{
			JobId:               jobId,
			RequestTime:         time.Now(),
			TransactionId:       model.NewRequestId(model.UShort(LastTransactionId.Add(1))),
			LogicDeviceId:       deviceId,
			RegisterDefinitions: *&registerDefinitions,
		}
		response, err := tcpHandler.FetchRegisterValues(&request)
		if err != nil {
			log.Default().Println("Error requesting register values:", err)
			return
		}
		err = storeResponse(response)
		if err != nil {
			log.Default().Println("Error storing response:", err)
			return
		}
	}
}

func listsOfConsecutiveRegisters(deviceTypes []string) (*[][]model.RegisterDefinition, error) {
	registerDefinitions, err := fetchRegisterDefinitionsFromMasterData(deviceTypes)
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

func fetchRegisterDefinitionsFromMasterData(deviceTypes []string) (*[]model.RegisterDefinition, error) {
	log.Default().Println("Getting register definitions for DeviceTypes", deviceTypes, "...")
	definitions, err := NewMasterDataService(config.MdConfig()).FetchRegisterDefinitions(deviceTypes, Categories)
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

func storeResponse(response *modbus.RegisterValueResponse) error {
	log.Default().Println("Response:", response)
	jsonResponse, err := json.MarshalIndent(response, "", "  ")
	if err != nil {
		return err
	}
	log.Default().Println("JSON Response:", string(jsonResponse))
	err = NewPersistingService(config.DataAccessorCfg()).StoreValues(response)
	if err != nil {
		return err
	}
	return nil
}
