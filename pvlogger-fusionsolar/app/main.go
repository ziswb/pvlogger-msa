package main

import (
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/fusionsolar"
	"pvlogger/internal/model"

	"pvlogger-go-shared/logging"
)

func main() {
	token, err := auth.Authenticate()
	if err != nil {
		logging.Fatal("authentication failed", "error", err)
	}
	name := config.JobConfig().JobName
	logging.Info("starting job", "name", name)
	fusionSolarAccess := fusionsolar.NewFusionSolarAccess(*token)
	defer fusionSolarAccess.Close()
	switch name {
	case model.StationRealKpi.String():
		response, err := fusionSolarAccess.FetchStationRealTimeData()
		if err != nil {
			logging.Fatal("failed to fetch station real time data", "error", err)
		}
		err = fusionSolarAccess.SaveStationRealKpiResponse(response)
		if err != nil {
			logging.Fatal("failed to save station real kpi response", "error", err)
		}
		break
	case model.DeviceRealKpi.String():
		response, err := fusionSolarAccess.FetchDeviceRealData()
		if err != nil {
			logging.Fatal("failed to fetch device real data", "error", err)
		}
		err = fusionSolarAccess.SaveDeviceRealKpiResponse(response)
		if err != nil {
			logging.Fatal("failed to save device real kpi response", "error", err)
		}
		break
	case model.StationDayKpi.String():
		response, err := fusionSolarAccess.FetchStationDayKpi()
		if err != nil {
			logging.Fatal("failed to fetch station day kpi", "error", err)
		}
		err = fusionSolarAccess.SaveStationDayKpiResponse(response)
		if err != nil {
			logging.Fatal("failed to save station day kpi response", "error", err)
		}
		break
	case model.StationMonthKpi.String():
		response, err := fusionSolarAccess.FetchMonthData()
		if err != nil {
			logging.Fatal("failed to fetch station month kpi", "error", err)
		}
		err = fusionSolarAccess.SaveStationMonthKpiResponse(response)
		if err != nil {
			logging.Fatal("failed to save station month kpi response", "error", err)
		}
		break
	case model.StationYearKpi.String():
		response, err := fusionSolarAccess.FetchYearData()
		if err != nil {
			logging.Fatal("failed to fetch station year kpi", "error", err)
		}
		err = fusionSolarAccess.SaveStationYearKpiResponse(response)
		if err != nil {
			logging.Fatal("failed to save station year kpi response", "error", err)
		}
		break
	default:
		logging.Fatal("unknown job name", "name", name)
	}
}
