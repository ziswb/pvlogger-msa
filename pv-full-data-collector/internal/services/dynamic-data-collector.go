package services

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/aggregate"
	"pvlogger-shared-modbus/collector"
	"pvlogger-shared-modbus/model"
)

func FetchDynamicDataAndAggregateKpis() error {
	jobId, err := FetchModbusDynamicRegisterValues()
	if err != nil {
		return err
	}
	// One KPI path (ADR-0016): the shared aggregator reads this run's rows
	// back from the data accessor, derives the KPIs and stores them. It
	// requires the dongle aggregate block in the run — the sweep only carries
	// it once masterdata has a DONGLE device type (backlog 35); until then
	// this step fails and the run exits non-zero.
	return aggregate.NewKpiAggregator().KpisFor(*jobId)
}

func FetchModbusDynamicRegisterValues() (*model.JobId, error) {
	logging.Info("fetching modbus dynamic register values")
	service := collector.NewReadRegisterValuesService([]model.Category{model.DynamicCategory, model.AlarmCategory})
	if err := service.ExecuteFetchRegisterValuesJob(); err != nil {
		return nil, err
	}
	return service.JobId(), nil
}
