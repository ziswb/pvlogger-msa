package model

type AduFrame struct {
	Header       MbapHeader        `json:"header"`
	FunctionCode FunctionCode      `json:"functionCode"`
	Data         ModbusRequestData `json:"data"`
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
