package services

import (
	"pv-full-data-collector/internal/config"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/model"
	"pv-full-data-collector/internal/services/dataaccessor"
	"pv-full-data-collector/internal/services/metrics"
	"pvlogger-shared-modbus/persistence"
	"pv-logger/logging"
	"time"
)

func AggregateKpisFor(jobId *model.JobId) error {
	logging.Info("aggregating KPIs", "job_id", jobId.Id)
	liveKpis, err := collectLiveKpis(jobId)
	if err != nil {
		return err
	}
	todayKpis, err := collectEnergiesTodaySoFar(jobId)
	if err != nil {
		return err
	}
	kpiValues := kpis.PvLoggerKpiData{
		JobId:         jobId.Id,
		DateCreated:   time.Now().UnixMilli(),
		Producing:     liveKpis.Producing(),
		Exporting:     liveKpis.Exporting(),
		Importing:     liveKpis.Importing(),
		Consuming:     liveKpis.Consuming(),
		SelfConsuming: liveKpis.SelfConsuming(),
		Produced:      todayKpis.Produced().ToWh(),
		Exported:      todayKpis.Exported().ToWh(),
		Imported:      todayKpis.Imported().ToWh(),
		Consumed:      todayKpis.Consumed().ToWh(),
		SelfConsumed:  todayKpis.SelfConsumed().ToWh(),
	}
	daCfg := config.DataAccessorCfg()
	if err := persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, daCfg.BasePath).StoreValues(&kpiValues, daCfg.StoreKpiValuesPath); err != nil {
		return err
	}
	pushKpiMetrics(&kpiValues)
	return nil
}

func pushKpiMetrics(kpiValues *kpis.PvLoggerKpiData) {
	cfg := sharedconfig.PushGatewayCfg()
	if !cfg.Enabled {
		logging.Debug("push gateway is disabled")
		return
	}
	metrics.NewMetricsPushService(cfg).PushKpiMetrics(kpiValues)
}

func collectLiveKpis(jobId *model.JobId) (*kpis.KpiLiveData, error) {
	myInverterActivePower, err := dataaccessor.NewDataAccessorService().InverterActivePower(jobId)
	if err != nil {
		return nil, err
	}
	myMeterActivePower, err := dataaccessor.NewDataAccessorService().MeterActivePower(jobId)
	if err != nil {
		return nil, err
	}
	return &kpis.KpiLiveData{
		InverterActivePower: myInverterActivePower,
		MeterActivePower:    myMeterActivePower,
	}, nil
}

func collectEnergiesTodaySoFar(jobId *model.JobId) (*kpis.KpiTodayData, error) {
	myInverterActivePower, err := dataaccessor.NewDataAccessorService().InverterEnergyToday(jobId)
	if err != nil {
		return nil, err
	}
	myMeterActivePower, err := dataaccessor.NewDataAccessorService().MeterEnergyToday()
	if err != nil {
		return nil, err
	}
	return &kpis.KpiTodayData{
		InverterTodayEnergy: myInverterActivePower,
		MeterTodayEnergy:    myMeterActivePower,
	}, nil
}
