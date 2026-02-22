package request

import (
	"pvlogger-shared-modbus/model"
)

func (r *ReadDeviceIdentifiersRequest) EncodeToBytes() []byte {
	dataLength := model.UShort(0x05)
	functionCode := model.ReadDeviceIdentifiers
	meiType := model.UByte(0x0E)
	readDevIdCode := r.ReadDevIdCode
	objectId := r.ObjectId
	mbapHeader := model.MbapHeader{
		ProtocolIdentifier: r.RequestId.Id,
		ProtocolType:       model.DefaultProtocolType,
		DataLength:         dataLength,
		LogicDeviceId:      r.LogicDeviceId,
	}
	data := model.NewModbusRequestDataFromUByteValues(meiType, readDevIdCode, objectId)
	aduFrame := model.NewAduFrame(mbapHeader, functionCode, data)
	return aduFrame.EncodeToBytes()
}
