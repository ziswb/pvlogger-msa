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
	switch name {
	case model.STATION_REAL_KPI.String():
		rtd, err := fusionsolar.FetchStationRealTimeData(*token)
		if err != nil {
			log.Fatal(err)
		}
		err = fusionsolar.SaveStationRealKpiResponse(rtd)
		if err != nil {
			log.Fatal(err)
		}
		break
	case model.DEVICE_REAL_KPI.String():
		rtd, err := fusionsolar.FetchDeviceRealData(*token)
		if err != nil {
			log.Fatal(err)
		}
		err = fusionsolar.SaveDeviceRealKpiResponse(rtd)
		if err != nil {
			log.Fatal(err)
		}
		break
	default:
		log.Default().Fatal("unknown job name")
	}
}
