package request

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/model"
)

func (r *ReadHoldingRegistersValueRequest) EncodeToBytes() []byte {
	logging.Info("encoding read holding register value request")
	if len(r.RegisterDefinitions) == 0 {
		logging.Warn("no register definitions found")
		return []byte{}
	}
	if logging.DebugEnabled() {
		logging.Debug("register definitions", "logic_device_id", r.LogicDeviceId)
		for _, regDef := range r.RegisterDefinitions {
			logging.Debug("register definition", "register_address", regDef.RegisterAddress.Value, "name", regDef.Name)
		}
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
