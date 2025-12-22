package services

import (
	"fmt"
	"log"
	"pvlogger-modbus-data-collector-go/app/internal/config"
	"pvlogger-modbus-data-collector-go/app/internal/modbus"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
)

const (
	MeterDeviceType    = "METER"
	InverterDeviceType = "INVERTER"
)
const (
	DynamicCategory = "DYNAMIC"
	AlarmCategory   = "ALARM"
)

var (
	Categories        = []string{DynamicCategory, AlarmCategory}
	LastTransactionId = atomic.Int32{}
)

type DeviceIdToTypesMap map[modbus.UByte][]string

func NewDeviceIdToTypesMap() DeviceIdToTypesMap {
	var deviceIdToTypesMap = make(map[modbus.UByte][]string)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().MeterDeviceIds, MeterDeviceType)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().InverterDeviceIds, InverterDeviceType)
	return deviceIdToTypesMap
}

func updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap map[modbus.UByte][]string, devIds []modbus.UByte, deviceType string) {
	for _, id := range devIds {
		if len(deviceIdToTypesMap[id]) == 0 {
			deviceIdToTypesMap[id] = []string{deviceType}
		} else {
			deviceIdToTypesMap[id] = append(deviceIdToTypesMap[id], deviceType)
		}
	}
}

func Execute() {
	var deviceIdToTypesMap = NewDeviceIdToTypesMap()
	jobId := uuid.New().String()
	log.Default().Println("Executing Job ID:", jobId)
	for deviceId, deviceTypes := range deviceIdToTypesMap {
		fetchRegisterValues(jobId, deviceId, deviceTypes)
	}
}

func fetchRegisterValues(jobId string, deviceId modbus.UByte, deviceTypes []string) {
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
		request := modbus.ReadHoldingRegistersValueRequest{
			JobId:               jobId,
			RequestTime:         time.Now(),
			TransactionId:       modbus.UShort(LastTransactionId.Add(1)),
			LogicDeviceId:       deviceId,
			RegisterDefinitions: *&registerDefinitions,
		}
		response, err := tcpHandler.FetchModbusRegisterValues(&request)
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

func listsOfConsecutiveRegisters(deviceTypes []string) (*[][]modbus.ModbusRegisterDefinition, error) {
	registerDefinitions, err := fetchRegisterDefinitionsFromMasterData(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions:", err)
		return &[][]modbus.ModbusRegisterDefinition{}, nil
	}
	var listsOfConsecutiveRegisters [][]modbus.ModbusRegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []modbus.ModbusRegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	log.Default().Println("Lists of consecutive registers:", len(listsOfConsecutiveRegisters))
	return &listsOfConsecutiveRegisters, nil
}

func fetchRegisterDefinitionsFromMasterData(deviceTypes []string) (*[]modbus.ModbusRegisterDefinition, error) {
	log.Default().Println("Getting register definitions for DeviceTypes", deviceTypes, "...")
	definitions, err := NewMasterDataService(config.MdConfig()).FetchRegisterDefinitions(deviceTypes, Categories)
	if err != nil {
		return &[]modbus.ModbusRegisterDefinition{}, err
	}
	log.Default().Printf("Got register definitions")
	return definitions, nil
}

func isNotSuccessor(registers [][]modbus.ModbusRegisterDefinition, nextRegister modbus.ModbusRegisterDefinition) bool {
	if len(registers) == 0 {
		return true
	}
	lastRegister := registers[len(registers)-1][len(registers[len(registers)-1])-1]
	lastRegisterAddress := lastRegister.RegisterAddress.Value
	nextRegisterAddress := lastRegisterAddress + lastRegister.Quantity.Value
	return nextRegister.RegisterAddress.Value != nextRegisterAddress
}

func storeResponse(response *modbus.ModbusRegisterValueResponse) error {
	log.Default().Println("Response:", response)
	return nil
}
