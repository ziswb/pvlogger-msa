package request

import (
	"pvlogger-shared-modbus/model"
	"time"
)

type ReadDeviceIdentifiersRequest struct {
	JobId         model.JobId
	RequestTime   time.Time
	RequestId     model.RequestId
	LogicDeviceId model.UByte
	ReadDevIdCode model.UByte
	ObjectId      model.UByte
}
