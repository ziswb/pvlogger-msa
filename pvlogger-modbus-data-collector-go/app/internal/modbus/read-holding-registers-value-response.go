package modbus

import (
	"encoding/binary"
	"log"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strings"
	"time"
)

// RegisterValue holds a decoded register value.
// Value contains the concrete Go type (UShort, UInt, Short, int32, string, etc.)
// determined at runtime based on RegisterDefinition.Type.
type RegisterValue struct {
	Definition model.RegisterDefinition `json:"definition"`
	Value      any                      `json:"value"`
}

func NewRegisterValue(definition model.RegisterDefinition, value any) *RegisterValue {
	return &RegisterValue{Definition: definition, Value: value}
}

type RegisterValueResponse struct {
	JobId         model.JobId        `json:"jobId"`
	TransactionId model.RequestId    `json:"requestId"`
	RequestTime   time.Time          `json:"requestTime"`
	ResponseTime  time.Time          `json:"responseTime"`
	Header        model.MbapHeader   `json:"header"`
	FunctionCode  model.FunctionCode `json:"functionCode"`
	NumberOfBytes model.UByte        `json:"numberOfBytes"`
	Values        []*RegisterValue   `json:"registerValues"`
}

func NewRegisterValueResponseFromBytes(regDefs []model.RegisterDefinition, jobId model.JobId, transactionId model.RequestId, requestTime time.Time, header model.MbapHeader, bytes []byte) *RegisterValueResponse {
	functionCode := model.NewFunctionCodeFromByte(bytes[0])
	numberOfBytes := bytes[1]
	values := readRegisterValues(regDefs, bytes[2:])
	return &RegisterValueResponse{
		JobId:         jobId,
		TransactionId: transactionId,
		RequestTime:   requestTime,
		ResponseTime:  time.Now(),
		Header:        header,
		FunctionCode:  functionCode,
		NumberOfBytes: numberOfBytes,
		Values:        values,
	}
}

// readRegisterValues decodes multiple register values from payload bytes.
// Each value's concrete Go type is determined by its RegisterDefinition.Type.
func readRegisterValues(registerDefinitions []model.RegisterDefinition, payload []byte) []*RegisterValue {
	var values []*RegisterValue
	offset := 0
	for _, registerDefinition := range registerDefinitions {
		byteCount := registerDefinition.Quantity.Value * 2 // each register is 2 bytes
		if offset+byteCount > len(payload) {
			log.Default().Printf("Error reading register values: offset %d + byte count %d > payload length %d", offset, byteCount, len(payload))
			break
		}
		registerValueBytes := payload[offset : offset+byteCount]
		if v := readRegisterValue(registerDefinition, registerValueBytes); v != nil {
			values = append(values, v)
		}
		offset += byteCount
	}
	return values
}

// readRegisterValue decodes a single register value.
// The returned RegisterValue.Value contains the concrete Go type:
//   - RegisterTypeU16 / RegisterTypeBitfield16 -> UShort (uint16)
//   - RegisterTypeU32 / RegisterTypeBitfield32 -> UInt (uint32)
//   - RegisterTypeI16 -> Short (int16)
//   - RegisterTypeI32 -> int32
//   - RegisterTypeSTR -> string
//   - RegisterTypeMLD -> []Short
func readRegisterValue(register model.RegisterDefinition, payload []byte) *RegisterValue {
	switch model.ToRegisterType(register.Type) {
	case model.RegisterTypeU16:
		if len(payload) < 2 {
			log.Default().Printf("Error reading register value: payload length %d < 2", len(payload))
			return nil
		}
		return NewRegisterValue(register, model.UShort(binary.BigEndian.Uint16(payload[0:2])))
	case model.RegisterTypeU32:
		if len(payload) < 4 {
			log.Default().Printf("Error reading register value: payload length %d < 4", len(payload))
			return nil
		}
		return NewRegisterValue(register, model.UInt(binary.BigEndian.Uint32(payload[0:4])))
	case model.RegisterTypeI16:
		if len(payload) < 2 {
			log.Default().Printf("Error reading register value: payload length %d < 2", len(payload))
			return nil
		}
		return NewRegisterValue(register, model.Short(int16(binary.BigEndian.Uint16(payload[0:2]))))
	case model.RegisterTypeI32:
		if len(payload) < 4 {
			log.Default().Printf("Error reading register value: payload length %d < 4", len(payload))
			return nil
		}
		return NewRegisterValue(register, int32(binary.BigEndian.Uint32(payload[0:4])))
	case model.RegisterTypeSTR:
		s := strings.TrimRight(string(payload), "\x00 ")
		return NewRegisterValue(register, s)
	case model.RegisterTypeBitfield16:
		if len(payload) < 2 {
			log.Default().Printf("Error reading register value: payload length %d < 2", len(payload))
			return nil
		}
		return NewRegisterValue(register, model.UShort(binary.BigEndian.Uint16(payload[0:2])))
	case model.RegisterTypeBitfield32:
		if len(payload) < 4 {
			log.Default().Printf("Error reading register value: payload length %d < 4", len(payload))
			return nil
		}
		return NewRegisterValue(register, model.UInt(binary.BigEndian.Uint32(payload[0:4])))
	case model.RegisterTypeMLD:
		if len(payload)%2 != 0 {
			log.Default().Printf("Error reading register value: payload length %d is not a multiple of 2", len(payload))
			return nil
		}
		var mld []model.Short
		for i, _ := range payload {
			if i%2 == 0 {
				mld = append(mld, model.Short(int16(binary.BigEndian.Uint16(payload[i:i+2]))))
			}
		}
		return NewRegisterValue(register, mld)
	}
	return nil
}
