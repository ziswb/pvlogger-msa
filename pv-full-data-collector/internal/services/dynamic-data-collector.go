package services

import (
	"pvlogger-shared-modbus/model"
	"pv-full-data-collector/internal/services/jobs"
	"pv-logger/logging"
)

func FetchDynamicDataAndAggregateKpis() {
	jobId := FetchModbusDynamicRegisterValues()
	AggregateKpisFor(jobId)
}

func FetchModbusDynamicRegisterValues() *model.JobId {
	logging.Info("fetching modbus dynamic register values")
	service := jobs.NewReadRegisterValuesService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	service.ExecuteFetchRegisterValuesJob()
	return service.JobId()
}
