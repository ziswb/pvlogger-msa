package modbus

type AduFrame struct {
	Header       MbapHeader
	FunctionCode FunctionCode
	Data         ModbusRequestData
}

func NewAduFrame(header MbapHeader, functionCode FunctionCode, data ModbusRequestData) AduFrame {
	return AduFrame{header, functionCode, data}
}

func (frame *AduFrame) EncodeToBytes() []byte {
	var bytes []byte
	bytes = append(bytes, frame.Header.EncodeToBytes()...)
	bytes = append(bytes, frame.FunctionCode.EncodeToBytes()...)
	bytes = append(bytes, frame.Data.EncodeToBytes()...)
	return bytes
}
