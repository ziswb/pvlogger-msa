package response

import (
	"encoding/binary"
	"log"
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
		s, err := charmap.ISO8859_5.NewDecoder().Bytes(payload)
		if err != nil {
			log.Default().Printf("Error reading register value: %v", err)
			return NewRegisterValue(register, "ERROR")
		}
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
