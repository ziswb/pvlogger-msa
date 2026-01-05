package services

import (
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-modbus-data-collector/internal/kpis"
	"pvlogger-modbus-data-collector/internal/logging"
	"pvlogger-modbus-data-collector/internal/modbus/model"
	"pvlogger-modbus-data-collector/internal/services/dataaccessor"
	"pvlogger-modbus-data-collector/internal/services/persistence"
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
	persistence.NewPersistingService(config.DataAccessorCfg()).StoreKpiValues(&kpiValues)
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
