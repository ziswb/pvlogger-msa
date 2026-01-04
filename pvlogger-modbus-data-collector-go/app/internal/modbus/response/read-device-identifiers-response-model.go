package response

import (
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strconv"
	"time"
)

// UByteArray is a custom type that serializes to a JSON array of integers
// instead of base64 (which is Go's default for []byte).
// This ensures compatibility with Kotlin/Ktor's UByteArray deserialization.
type UByteArray []model.UByte

func (u UByteArray) MarshalJSON() ([]byte, error) {
	if u == nil {
		return []byte("null"), nil
	}
	if len(u) == 0 {
		return []byte("[]"), nil
	}
	// Manually build JSON array to avoid base64 encoding
	result := make([]byte, 0, len(u)*4+2)
	result = append(result, '[')
	for i, v := range u {
		if i > 0 {
			result = append(result, ',')
		}
		result = append(result, []byte(strconv.FormatUint(uint64(v), 10))...)
	}
	result = append(result, ']')
	return result, nil
}

type ModbusObject struct {
	ObjectId     model.UByte `json:"objectId"`
	ObjectLength model.UByte `json:"objectLength"`
	ObjectValue  UByteArray  `json:"objectValue"`
}

func NewModbusObject(objectId model.UByte, objectLength model.UByte, objectValue []model.UByte) *ModbusObject {
	return &ModbusObject{
		ObjectId:     objectId,
		ObjectLength: objectLength,
		ObjectValue:  UByteArray(objectValue),
	}
}

type ReadDeviceIdentifiersResponse struct {
	JobId            model.JobId        `json:"jobId"`
	RequestId        model.RequestId    `json:"requestId"`
	RequestTime      time.Time          `json:"requestTime"`
	ResponseTime     time.Time          `json:"responseTime"`
	Header           model.MbapHeader   `json:"header"`
	FunctionCode     model.FunctionCode `json:"functionCode"`
	NumberOfBytes    model.UByte        `json:"numberOfBytes"`
	MeiType          model.UByte        `json:"meiType"`
	ReadDevIdCode    model.UByte        `json:"readDevIdCode"`
	ConsistencyLevel model.UByte        `json:"consistencyLevel"`
	More             model.UByte        `json:"more"`
	NextObjectId     model.UByte        `json:"nextObjectId"`
	NumberOfObjects  model.UByte        `json:"numberOfObjects"`
	ObjectList       []*ModbusObject    `json:"objectList"`
}
