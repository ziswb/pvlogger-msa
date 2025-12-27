package request

import (
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"time"
)

type ReadHoldingRegistersValueRequest struct {
	JobId               model.JobId
	RequestTime         time.Time
	TransactionId       model.RequestId
	LogicDeviceId       model.UByte
	RegisterDefinitions []model.RegisterDefinition
}
