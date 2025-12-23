package services

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"time"
)

func AggregateKpisFor(jobId *modbus.JobId) *kpis.PvLoggerKpiData {
	log.Default().Println("Aggregating KPIs Job ID:", jobId)
	liveKpis := collectLiveKpis(jobId)
	todayKpis := collectEnergiesTodaySoFar(jobId)
	return &kpis.PvLoggerKpiData{
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
}

func collectLiveKpis(jobId *modbus.JobId) *kpis.KpiLiveData {
	myInverterActivePower := InverterActivePower(jobId)
	myMeterActivePower := MeterActivePower(jobId)
	return &kpis.KpiLiveData{
		InverterActivePower: myInverterActivePower,
		MeterActivePower:    myMeterActivePower,
	}
}

func collectEnergiesTodaySoFar(jobId *modbus.JobId) *kpis.KpiTodayData {
	myInverterActivePower := InverterEnergyToday(jobId)
	myMeterActivePower := MeterEnergyToday()
	return &kpis.KpiTodayData{
		InverterTodayEnergy: myInverterActivePower,
		MeterTodayEnergy:    myMeterActivePower,
	}
}
