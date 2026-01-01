package response

import (
	"encoding/base64"
	"encoding/binary"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"time"

	"golang.org/x/text/encoding/charmap"
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
	logging.Debug("handling STR", "raw_payload", string(payload))
	s, err := charmap.ISO8859_5.NewDecoder().Bytes(payload)
	if err != nil {
		logging.Fatal("error reading register value", "error", err)
	}
	logging.Debug("handling STR", "iso8859_5_value", string(s))
	text := make([]byte, base64.StdEncoding.DecodedLen(len(s)))
	decodedBytes, err := base64.StdEncoding.Decode(text, s)
	if err != nil {
		logging.Fatal("error decoding string register value", "error", err)
	}
	text = text[:decodedBytes]
	logging.Debug("handling STR", "decoded_base64", string(text))
	return NewRegisterValue(register, text)
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
	for i, _ := range payload {
		if i%2 == 0 {
			mld = append(mld, model.Short(int16(binary.BigEndian.Uint16(payload[i:i+2]))))
		}
	}
	return NewRegisterValue(register, mld)
}
