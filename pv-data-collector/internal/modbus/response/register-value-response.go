package response

import (
	"encoding/binary"
	"pv-data-collector/internal/modbus/model"
	"pvlogger-go-shared/logging"
	"time"
)

type RegisterValue struct {
	Definition model.RegisterDefinition `json:"definition"`
	Value      any                      `json:"value"`
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

func readRegisterValues(registerDefinitions []model.RegisterDefinition, payload []byte) []*RegisterValue {
	logging.Info("reading register values")
	var values []*RegisterValue
	offset := 0
	for _, registerDefinition := range registerDefinitions {
		byteCount := registerDefinition.Quantity.Value * 2
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
		offset += byteCount
	}
	return values
}

func readRegisterValue(register model.RegisterDefinition, payload []byte) *RegisterValue {
	switch model.ToRegisterType(register.Type) {
	case model.RegisterTypeU32:
		return handleU32(register, payload)
	case model.RegisterTypeI32:
		return handleI32(register, payload)
	}
	return nil
}

func handleU32(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 4 {
		logging.Error("error reading register value", "payload_length", len(payload), "required", 4)
		return nil
	}
	return &RegisterValue{Definition: register, Value: model.UInt(binary.BigEndian.Uint32(payload[0:4]))}
}

func handleI32(register model.RegisterDefinition, payload []byte) *RegisterValue {
	if len(payload) < 4 {
		logging.Fatal("error reading register value", "payload_length", len(payload), "required", 4)
	}
	return &RegisterValue{Definition: register, Value: int32(binary.BigEndian.Uint32(payload[0:4]))}
}
