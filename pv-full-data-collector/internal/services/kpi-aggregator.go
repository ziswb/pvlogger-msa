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

func AggregateKpisFor(jobId *model.JobId) {
	logging.Info("aggregating KPIs", "job_id", jobId.Id)
	liveKpis := collectLiveKpis(jobId)
	todayKpis := collectEnergiesTodaySoFar(jobId)
	kpiValues := kpis.PvLoggerKpiData{
		JobId:         jobId.Id,
		DateCreated:   time.Now().Add(1 * time.Hour).UnixMilli(),
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
	persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, daCfg.BasePath).StoreValues(&kpiValues, daCfg.StoreKpiValuesPath)
	pushKpiMetrics(&kpiValues)
}

func pushKpiMetrics(kpiValues *kpis.PvLoggerKpiData) {
	cfg := sharedconfig.PushGatewayCfg()
	if !cfg.Enabled {
		logging.Debug("push gateway is disabled")
		return
	}
	metrics.NewMetricsPushService(cfg).PushKpiMetrics(kpiValues)
}

func collectLiveKpis(jobId *model.JobId) *kpis.KpiLiveData {
	myInverterActivePower := dataaccessor.NewDataAccessorService().InverterActivePower(jobId)
	myMeterActivePower := dataaccessor.NewDataAccessorService().MeterActivePower(jobId)
	return &kpis.KpiLiveData{
		InverterActivePower: myInverterActivePower,
		MeterActivePower:    myMeterActivePower,
	}
}

func collectEnergiesTodaySoFar(jobId *model.JobId) *kpis.KpiTodayData {
	myInverterActivePower := dataaccessor.NewDataAccessorService().InverterEnergyToday(jobId)
	myMeterActivePower := dataaccessor.NewDataAccessorService().MeterEnergyToday()
	return &kpis.KpiTodayData{
		InverterTodayEnergy: myInverterActivePower,
		MeterTodayEnergy:    myMeterActivePower,
	}
}
