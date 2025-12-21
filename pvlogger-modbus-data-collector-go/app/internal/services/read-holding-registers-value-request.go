package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/app/internal/modbus"
	"time"
)

type ReadHoldingRegistersValueRequest struct {
	JobId               string
	RequestTime         time.Time
	RequestIdId         modbus.UShort
	DeviceId            modbus.UByte
	RegisterDefinitions []modbus.ModbusRegisterDefinition
}

func (r *ReadHoldingRegistersValueRequest) EncodeToBytes() []byte {
	if len(r.RegisterDefinitions) == 0 {
		log.Default().Println("No register definitions found")
		return []byte{}
	}
	registerAddress := modbus.UShort(r.RegisterDefinitions[0].RegisterAddress.Value)
	numberOfBytes := modbus.UShort(0)
	for _, regDef := range r.RegisterDefinitions {
		numberOfBytes += modbus.UShort(regDef.Quantity.Value)
	}
	modbusData := modbus.ModbusRequestData{
		StartingAddress:   registerAddress,
		NumberOfRegisters: numberOfBytes,
	}
	mbapHeader := modbus.MbapHeader{
		ProtocolIdentifier: r.RequestIdId,
		ProtocolType:       modbus.DefaultProtocolType,
		DataLength:         modbus.UShort(2 + modbusData.NumberOfBytes()), // logic device id + function code + payload
		LogicDeviceId:      r.DeviceId,
	}
	functionCode := modbus.ReadRegisters
	aduFrame := modbus.NewAduFrame(mbapHeader, functionCode, modbusData)
	return aduFrame.EncodeToBytes()
}
