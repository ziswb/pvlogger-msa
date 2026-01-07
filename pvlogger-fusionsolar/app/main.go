package main

import (
	"log"
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/fusionsolar"
	"pvlogger/internal/model"
)

func main() {
	token, err := auth.Authenticate()
	if err != nil {
		log.Fatal(err)
	}
	name := config.JobConfig().JobName
	log.Default().Println("name: " + name)
	fusionSolarAccess := fusionsolar.NewFusionSolarAccess(*token)
	defer fusionSolarAccess.Close()
	switch name {
	case model.STATION_REAL_KPI.String():
		err := fusionSolarAccess.FetchStationRealTimeData()
		if err != nil {
			log.Fatal(err)
		}
		err = fusionSolarAccess.SaveStationRealKpiResponse()
		if err != nil {
			log.Fatal(err)
		}
		break
	case model.DEVICE_REAL_KPI.String():
		err := fusionSolarAccess.FetchDeviceRealData()
		if err != nil {
			log.Fatal(err)
		}
		err = fusionSolarAccess.SaveDeviceRealKpiResponse()
		if err != nil {
			log.Fatal(err)
		}
		break
	default:
		log.Default().Fatal("unknown job name")
	}
}
