package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"pvlogger-modbus-data-collector-go/internal/services/dataaccessor"
	"pvlogger-modbus-data-collector-go/internal/services/persistence"
	"time"
)

func AggregateKpisFor(jobId *model.JobId) {
	log.Default().Println("Aggregating KPIs Job ID:", jobId)
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
	err := persistence.NewPersistingService(config.DataAccessorCfg()).StoreKpiValues(&kpiValues)
	if err != nil {
		log.Default().Println("Failed to store kpi values:", err)
		return
	}
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
