package request

import (
	"pvlogger-go-shared/logging"
	"pvlogger-modbus-data-collector/internal/modbus/model"
)

func (r *ReadHoldingRegistersValueRequest) EncodeToBytes() []byte {
	if len(r.RegisterDefinitions) == 0 {
		logging.Warn("no register definitions found")
		return []byte{}
	}
	registerAddress := model.UShort(r.RegisterDefinitions[0].RegisterAddress.Value)
	numberOfBytes := model.UShort(0)
	for _, regDef := range r.RegisterDefinitions {
		numberOfBytes += model.UShort(regDef.Quantity.Value)
	}
	modbusData := model.NewModbusRequestDataFromUShortValues(registerAddress, numberOfBytes)
	mbapHeader := model.MbapHeader{
		ProtocolIdentifier: r.RequestId.Id,
		ProtocolType:       model.DefaultProtocolType,
		DataLength:         model.UShort(2 + modbusData.NumberOfBytes()), // logic device id + function code + payload
		LogicDeviceId:      r.LogicDeviceId,
	}
	functionCode := model.ReadRegisters
	aduFrame := model.NewAduFrame(mbapHeader, functionCode, modbusData)
	return aduFrame.EncodeToBytes()
}
