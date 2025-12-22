package modbus

import "time"

type ModbusRegisterValueResponse struct {
	JobId         string
	RequestId     uint16
	RequestTime   time.Time
	ResponseTime  time.Time
	Header        MbapHeader
	FunctionCode  FunctionCode
	NumberOfBytes uint16
	Data          []byte
}

type Value interface {
	int | []Short | Short | string | UInt | UShort
}

type ModbusRegisterValue[V Value] struct {
	Definition ModbusRegisterDefinition
	Value      V
}
