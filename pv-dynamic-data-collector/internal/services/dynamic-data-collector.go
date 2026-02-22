package services

import (
	"pvlogger-shared-modbus/model"
	"pv-dynamic-data-collector/internal/services/jobs"
	"pvlogger-go-shared/logging"
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
