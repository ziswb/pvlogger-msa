package response

import (
	"pvlogger-shared-modbus/model"
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
