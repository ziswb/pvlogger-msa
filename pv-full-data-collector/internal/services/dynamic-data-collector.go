package services

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/collector"
	"pvlogger-shared-modbus/model"
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
	service := collector.NewReadRegisterValuesService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	if err := service.ExecuteFetchRegisterValuesJob(); err != nil {
		return nil, err
	}
	return service.JobId(), nil
}
