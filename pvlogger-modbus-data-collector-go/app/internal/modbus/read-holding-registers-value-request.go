package modbus

import (
	"log"
	"time"
)

type ReadHoldingRegistersValueRequest struct {
	JobId               string
	RequestTime         time.Time
	TransactionId       UShort
	DeviceId            UByte
	RegisterDefinitions []ModbusRegisterDefinition
}

func (r *ReadHoldingRegistersValueRequest) EncodeToBytes() []byte {
	if len(r.RegisterDefinitions) == 0 {
		log.Default().Println("No register definitions found")
		return []byte{}
	}
	registerAddress := UShort(r.RegisterDefinitions[0].RegisterAddress.Value)
	numberOfBytes := UShort(0)
	for _, regDef := range r.RegisterDefinitions {
		numberOfBytes += UShort(regDef.Quantity.Value)
	}
	modbusData := ModbusRequestData{
		StartingAddress:   registerAddress,
		NumberOfRegisters: numberOfBytes,
	}
	mbapHeader := MbapHeader{
		ProtocolIdentifier: r.TransactionId,
		ProtocolType:       DefaultProtocolType,
		DataLength:         UShort(2 + modbusData.NumberOfBytes()), // logic device id + function code + payload
		LogicDeviceId:      r.DeviceId,
	}
	functionCode := ReadRegisters
	aduFrame := NewAduFrame(mbapHeader, functionCode, modbusData)
	return aduFrame.EncodeToBytes()
}
