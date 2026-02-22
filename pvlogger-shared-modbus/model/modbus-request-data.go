package model

import (
	"encoding/binary"
)

const bytesPerUShort = 2

type ModbusRequestData struct {
	data []byte
}

func NewModbusRequestDataFromUShortValues(values ...UShort) ModbusRequestData {
	return ModbusRequestData{
		data: dataFromUShorts(values...),
	}
}

func NewModbusRequestDataFromUByteValues(values ...UByte) ModbusRequestData {
	return ModbusRequestData{
		data: dataFromUBytes(values...),
	}
}

func (m *ModbusRequestData) NumberOfBytes() int {
	return len(m.data)
}

func (m *ModbusRequestData) EncodeToBytes() []byte {
	return m.data
}

func dataFromUBytes(values ...UByte) []byte {
	var d = make([]byte, len(values))
	copy(d, values)
	return d
}

func dataFromUShorts(values ...UShort) []byte {
	var d = make([]byte, len(values)*bytesPerUShort)
	for i, value := range values {
		binary.BigEndian.PutUint16(d[i*bytesPerUShort:], value)
	}
	return d
}
