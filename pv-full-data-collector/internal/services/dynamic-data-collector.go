package services

import (
	"pvlogger-shared-modbus/model"
	"pv-full-data-collector/internal/services/jobs"
	"pv-logger/logging"
)

func FetchDynamicDataAndAggregateKpis() error {
	jobId, err := FetchModbusDynamicRegisterValues()
	if err != nil {
		return err
	}
	return AggregateKpisFor(jobId)
}

func FetchModbusDynamicRegisterValues() (*model.JobId, error) {
	logging.Info("fetching modbus dynamic register values")
	service := jobs.NewReadRegisterValuesService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	if err := service.ExecuteFetchRegisterValuesJob(); err != nil {
		return nil, err
	}
	return service.JobId(), nil
}
