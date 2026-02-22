package response

import (
	"bytes"
	"encoding/binary"
	"pv-dynamic-data-collector/internal/modbus/model"
	"pvlogger-go-shared/logging"
	"strings"
	"time"
)

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
	logging.Info("reading register values")
	var values []*RegisterValue
	offset := 0
	for _, registerDefinition := range registerDefinitions {
		byteCount := registerDefinition.Quantity.Value * 2 // each register is 2 bytes
		if offset+byteCount > len(payload) {
			logging.Error("error reading register values", "offset", offset, "byte_count", byteCount, "payload_length", len(payload))
			break
		}
		registerValueBytes := payload[offset : offset+byteCount]
		if v := readRegisterValue(registerDefinition, registerValueBytes); v != nil {
			values = append(values, v)
			if logging.DebugEnabled() {
				logging.Debug("register value", "register_address", registerDefinition.RegisterAddress, "value", v.Value)
			}
		}
		if logging.DebugEnabled() {
			logging.Debug("register value", "register_address", registerDefinition.RegisterAddress, "value", "<nil>")
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
		return handleU16(register, payload)
	case model.RegisterTypeU32:
		return handleU32(register, payload)
	case model.RegisterTypeI16:
		return handleI16(register, payload)
	case model.RegisterTypeI32:
		return handleI32(register, payload)
	case model.RegisterTypeSTR:
		return handleStr(register, payload)
	case model.RegisterTypeBitfield16:
		return handleBitField16(register, payload)
	case model.RegisterTypeBitfield32:
		return handleBitField32(register, payload)
	case model.RegisterTypeMLD:
		return handleMld(register, payload)
	}
	return nil
}

func handleU16(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 2 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 2)
		return nil
	}
	return NewRegisterValue(register, model.UShort(binary.BigEndian.Uint16(payload[0:2])))
}

func handleU32(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 4 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 4)
		return nil
	}
	return NewRegisterValue(register, model.UInt(binary.BigEndian.Uint32(payload[0:4])))
}

func handleI16(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 2 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 2)
		return nil
	}
	return NewRegisterValue(register, model.Short(int16(binary.BigEndian.Uint16(payload[0:2]))))
}

func handleI32(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 4 {
		logging.Fatal("error reading register value", "payload_length", len(payload), "required", 4)
	}
	return NewRegisterValue(register, int32(binary.BigEndian.Uint32(payload[0:4])))
}

func handleStr(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if logging.DebugEnabled() {
		logging.Debug("handling STR", "raw_payload", payload)
	}
	decodedString := decodeHuaweiModbusString(payload)
	if logging.DebugEnabled() {
		logging.Debug("handling STR", "decoded_payload", decodedString)
	}
	return NewRegisterValue(register, decodedString)
}

// decodeHuaweiModbusString decodes a fixed-length Huawei Modbus string.
//   - payload: raw bytes read from Modbus registers
//   - NULL (0x00) bytes are treated as spaces
//   - Multiple trailing spaces are trimmed
func decodeHuaweiModbusString(payload []byte) string {
	// Replace any NULL with space
	for i, b := range payload {
		if b == 0x00 {
			payload[i] = ' '
		}
	}
	// Collapse internal multiple spaces to a single space
	out := bytes.Fields(payload) // splits on spaces
	if len(out) == 0 {
		return ""
	}
	// Rejoin with single space
	return strings.Join(bytesToStrings(out), " ")
}

func bytesToStrings(b [][]byte) []string {
	s := make([]string, len(b))
	for i := range b {
		s[i] = string(b[i])
	}
	return s
}

func handleBitField16(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 2 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 2)
		return nil
	}
	return NewRegisterValue(register, model.UShort(binary.BigEndian.Uint16(payload[0:2])))
}

func handleBitField32(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 4 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 4)
		return nil
	}
	return NewRegisterValue(register, model.UInt(binary.BigEndian.Uint32(payload[0:4])))
}

func handleMld(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload)%2 != 0 {
		logging.Error("error reading register value", "payload_length", len(payload), "issue", "not a multiple of 2")
		return nil
	}
	var mld []model.Short
	for i := range payload {
		if i%2 == 0 {
			mld = append(mld, model.Short(int16(binary.BigEndian.Uint16(payload[i:i+2]))))
		}
	}
	return NewRegisterValue(register, mld)
}
