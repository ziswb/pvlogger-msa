package services

import (
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/services/jobs"
)

func FetchModbusDynamicRegisterValues() *model.JobId {
	logging.Info("fetching modbus dynamic register values")
	service := jobs.NewRequestService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	service.ExecuteFetchRegisterValuesJob()
	return service.JobId()
}
