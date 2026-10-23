package services

import (
	"pv-logger/logging"
	"pvlogger-shared-modbus/aggregate"
	"pvlogger-shared-modbus/collector"
	"pvlogger-shared-modbus/model"
)

// sweepCategories are the register categories the hourly sweep reads.
var sweepCategories = []model.Category{model.DynamicCategory, model.AlarmCategory}

func FetchDynamicDataAndAggregateKpis() error {
	return runSweep(FetchModbusDynamicRegisterValues, func(jobId model.JobId) error {
		return aggregate.NewKpiAggregator().KpisFor(jobId)
	})
}

// runSweep reads the registers, then derives the KPIs from exactly that run.
// One KPI path (ADR-0016): the shared aggregator reads this run's rows back
// from the data accessor, derives the KPIs and stores them. It requires the
// dongle aggregate block in the run — the sweep only carries it once
// masterdata has a DONGLE device type (backlog 35); until then this step
// fails and the run exits non-zero. A failed read never aggregates.
func runSweep(fetch func() (*model.JobId, error), aggregateKpis func(model.JobId) error) error {
	jobId, err := fetch()
	if err != nil {
		return err
	}
	return aggregateKpis(*jobId)
}

func FetchModbusDynamicRegisterValues() (*model.JobId, error) {
	logging.Info("fetching modbus dynamic register values")
	service := collector.NewReadRegisterValuesService(sweepCategories)
	if err := service.ExecuteFetchRegisterValuesJob(); err != nil {
		return nil, err
	}
	return service.JobId(), nil
}
