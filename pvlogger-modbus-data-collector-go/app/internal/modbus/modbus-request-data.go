package modbus

import (
	"encoding/binary"
)

const bytesPerUShort = 2

type ModbusRequestData struct {
	StartingAddress   UShort
	NumberOfRegisters UShort
}

func (m *ModbusRequestData) NumberOfBytes() int {
	return 2 * bytesPerUShort
}

func (m *ModbusRequestData) EncodeToBytes() []byte {
	var d = make([]byte, m.NumberOfBytes())
	binary.BigEndian.PutUint16(d[0:], m.StartingAddress)
	binary.BigEndian.PutUint16(d[2:], m.NumberOfRegisters)
	return d
}
