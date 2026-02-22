package request

import (
	"pvlogger-shared-modbus/model"
	"time"
)

type ReadHoldingRegistersValueRequest struct {
	JobId               model.JobId
	RequestTime         time.Time
	RequestId           model.RequestId
	LogicDeviceId       model.UByte
	RegisterDefinitions []model.RegisterDefinition
}
