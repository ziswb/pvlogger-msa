package modbus

import (
	"encoding/binary"
	"strings"
	"time"
)

// ModbusRegisterValue holds a decoded register value.
// Value contains the concrete Go type (UShort, UInt, Short, int32, string, etc.)
// determined at runtime based on ModbusRegisterDefinition.Type.
type ModbusRegisterValue struct {
	Definition ModbusRegisterDefinition
	Value      any
}

func NewModbusRegisterValue(definition ModbusRegisterDefinition, value any) *ModbusRegisterValue {
	return &ModbusRegisterValue{Definition: definition, Value: value}
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
	Values        []*ModbusRegisterValue
}

func NewModbusRegisterValueResponseFromBytes(regDefs []ModbusRegisterDefinition, jobId string, transactionId uint16, requestTime time.Time, header MbapHeader, bytes []byte) *ModbusRegisterValueResponse {
	data := bytes[2:]
	values := readRegisterValues(regDefs, data)
	return &ModbusRegisterValueResponse{
		JobId:         jobId,
		TransactionId: transactionId,
		RequestTime:   requestTime,
		ResponseTime:  time.Now(),
		Header:        header,
		FunctionCode:  NewFunctionCodeFromByte(bytes[0]),
		NumberOfBytes: bytes[1],
		Data:          data,
		Values:        values,
	}
}

// readRegisterValues decodes multiple register values from payload bytes.
// Each value's concrete Go type is determined by its ModbusRegisterDefinition.Type.
func readRegisterValues(regDefs []ModbusRegisterDefinition, payload []byte) []*ModbusRegisterValue {
	var values []*ModbusRegisterValue
	offset := 0
	for _, regDef := range regDefs {
		byteCount := regDef.Quantity.Value * 2 // each register is 2 bytes
		if offset+byteCount > len(payload) {
			break
		}
		regBytes := payload[offset : offset+byteCount]
		if v := readRegisterValue(regDef, regBytes); v != nil {
			values = append(values, v)
		}
		offset += byteCount
	}
	return values
}

// readRegisterValue decodes a single register value.
// The returned ModbusRegisterValue.Value contains the concrete Go type:
//   - RegisterTypeU16 / RegisterTypeBitfield16 -> UShort (uint16)
//   - RegisterTypeU32 / RegisterTypeBitfield32 -> UInt (uint32)
//   - RegisterTypeI16 -> Short (int16)
//   - RegisterTypeI32 -> int32
//   - RegisterTypeSTR -> string
//   - RegisterTypeMLD -> []Short
func readRegisterValue(register ModbusRegisterDefinition, payload []byte) *ModbusRegisterValue {
	switch ToRegisterType(register.Type) {
	case RegisterTypeU16:
		if len(payload) < 2 {
			return nil
		}
		return NewModbusRegisterValue(register, UShort(binary.BigEndian.Uint16(payload[0:2])))
	case RegisterTypeU32:
		if len(payload) < 4 {
			return nil
		}
		return NewModbusRegisterValue(register, UInt(binary.BigEndian.Uint32(payload[0:4])))
	case RegisterTypeI16:
		if len(payload) < 2 {
			return nil
		}
		return NewModbusRegisterValue(register, Short(int16(binary.BigEndian.Uint16(payload[0:2]))))
	case RegisterTypeI32:
		if len(payload) < 4 {
			return nil
		}
		return NewModbusRegisterValue(register, int32(binary.BigEndian.Uint32(payload[0:4])))
	case RegisterTypeSTR:
		s := strings.TrimRight(string(payload), "\x00 ")
		return NewModbusRegisterValue(register, s)
	case RegisterTypeBitfield16:
		if len(payload) < 2 {
			return nil
		}
		return NewModbusRegisterValue(register, UShort(binary.BigEndian.Uint16(payload[0:2])))
	case RegisterTypeBitfield32:
		if len(payload) < 4 {
			return nil
		}
		return NewModbusRegisterValue(register, UInt(binary.BigEndian.Uint32(payload[0:4])))
	case RegisterTypeMLD:
		if len(payload)%2 != 0 {
			return nil
		}
		var mld []Short
		for i, _ := range payload {
			if i%2 == 0 {
				mld = append(mld, Short(int16(binary.BigEndian.Uint16(payload[i:i+2]))))
			}
		}
		return NewModbusRegisterValue(register, mld)
	}
	return nil
}
