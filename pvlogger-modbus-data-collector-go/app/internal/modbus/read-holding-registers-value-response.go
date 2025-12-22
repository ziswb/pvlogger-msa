package modbus

import "time"

type Value interface {
	int | []Short | Short | string | UInt | UShort
}

type ModbusRegisterValue[V Value] struct {
	Definition ModbusRegisterDefinition
	Value      V
}

type ModbusRegisterValueResponse struct {
	JobId         string
	TransactionId uint16
	RequestTime   time.Time
	ResponseTime  time.Time
	Header        MbapHeader
	FunctionCode  FunctionCode
	NumberOfBytes UByte
	Data          []byte
}

func NewModbusRegisterValueResponseFromBytes(jobId string, transactionId uint16, requestTime time.Time, header MbapHeader, bytes []byte) *ModbusRegisterValueResponse {
	return &ModbusRegisterValueResponse{
		JobId:         jobId,
		TransactionId: transactionId,
		RequestTime:   requestTime,
		ResponseTime:  time.Now(),
		Header:        header,
		FunctionCode:  NewFunctionCodeFromByte(bytes[0]),
		NumberOfBytes: bytes[1],
		Data:          bytes[2:],
	}
}
