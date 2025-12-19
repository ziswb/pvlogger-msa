package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/app/internal/config"
	"pvlogger-modbus-data-collector-go/app/internal/model"
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
	Categories = []string{DynamicCategory, AlarmCategory}
	RequestId  = atomic.Int32{}
)

func Execute() {
	log.Default().Println("Executing modbus batched dynamic data collection job ...")
	var deviceIdToTypesMap = make(map[model.UByte][]string)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().MeterDeviceIds, MeterDeviceType)
	updateDeviceIdToDeviceTypesMap(deviceIdToTypesMap, config.DevIdConfig().InverterDeviceIds, InverterDeviceType)
	for deviceId, deviceTypes := range deviceIdToTypesMap {
		log.Default().Println("Fetching register values for Device ID: '", deviceId, "' having Device Types: ", deviceTypes)
		fetchRegisterValues(uuid.New().String(), deviceId, deviceTypes)
	}
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

func fetchRegisterValues(jobId string, deviceId model.UByte, deviceTypes []string) {
	log.Default().Println("Fetching register values for Device ID: '", deviceId, "' having Device Types: ", deviceTypes)
	listOfRegDefs, err := listsOfConsecutiveRegisters(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions: ", err)
		return
	}
	for idx, regDefs := range listOfRegDefs {
		request := buildRequest(jobId, deviceId, &regDefs)
		log.Default().Println("Request ", idx, " -> ", request.RequestIdId, " -> ", len(*request.RegisterDefinitions))
	}
}

func listsOfConsecutiveRegisters(deviceTypes []string) ([][]model.ModbusRegisterDefinition, error) {
	registerDefinitions, err := registerDefinitions(deviceTypes)
	if err != nil {
		log.Default().Println("Error getting register definitions: ", err)
		return [][]model.ModbusRegisterDefinition{}, nil
	}
	var listsOfConsecutiveRegisters [][]model.ModbusRegisterDefinition
	for _, registerDefinition := range *registerDefinitions {
		if isNotSuccessor(listsOfConsecutiveRegisters, registerDefinition) {
			listsOfConsecutiveRegisters = append(listsOfConsecutiveRegisters, []model.ModbusRegisterDefinition{registerDefinition})
		} else {
			listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1] = append(listsOfConsecutiveRegisters[len(listsOfConsecutiveRegisters)-1], registerDefinition)
		}
	}
	log.Default().Println("Lists of consecutive registers: ", len(listsOfConsecutiveRegisters))
	for id, list := range listsOfConsecutiveRegisters {
		log.Default().Println("List ", id, " -> register definitions:", len(list))
	}
	return listsOfConsecutiveRegisters, nil
}

func registerDefinitions(deviceTypes []string) (*[]model.ModbusRegisterDefinition, error) {
	log.Default().Println("Getting register definitions for DeviceTypes '", deviceTypes, "'...")
	host := config.MdConfig().Host
	port := config.MdConfig().Port
	proto := config.MdConfig().Protocol
	definitions, err := NewMasterDataService(&MasterDataConfig{Protocol: proto, Host: host, Port: port}).FetchRegisterDefinitions(deviceTypes, Categories)
	if err != nil {
		return &[]model.ModbusRegisterDefinition{}, err
	}
	log.Default().Printf("Got register definitions")
	return definitions, nil
}

func isNotSuccessor(registers [][]model.ModbusRegisterDefinition, nextRegister model.ModbusRegisterDefinition) bool {
	if len(registers) == 0 {
		return true
	}
	lastRegister := registers[len(registers)-1][len(registers[len(registers)-1])-1]
	lastRegisterAddress := lastRegister.RegisterAddress.Value
	nextRegisterAddress := lastRegisterAddress + lastRegister.Quantity.Value
	return nextRegister.RegisterAddress.Value != nextRegisterAddress
}

func buildRequest(jobId string, deviceId model.UByte, regDefs *[]model.ModbusRegisterDefinition) *model.ReadRegistersValueRequest {
	return &model.ReadRegistersValueRequest{
		JobId:               jobId,
		RequestTime:         time.Now(),
		RequestIdId:         model.UShort(RequestId.Add(1)),
		DeviceId:            deviceId,
		RegisterDefinitions: regDefs,
	}
}
