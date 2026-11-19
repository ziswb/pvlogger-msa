package jobs

import (
	"pvlogger-fusionsolar/internal/fusionsolar"
	"pvlogger-fusionsolar/internal/model"

	"pv-logger/logging"
)

func Run(name string, access *fusionsolar.FusionSolarAccess) {
	switch name {
	case model.StationRealKpi.String():
		runStationRealKpi(access)
	case model.DeviceRealKpi.String():
		runDeviceRealKpi(access)
	case model.StationDayKpi.String():
		runStationDayKpi(access)
	case model.StationMonthKpi.String():
		runStationMonthKpi(access)
	case model.StationYearKpi.String():
		runStationYearKpi(access)
	case model.Device5MinKpi.String():
		runDevice5MinKpi(access)
	default:
		logging.Fatal("unknown job name", "name", name)
	}
}

func runStationRealKpi(access *fusionsolar.FusionSolarAccess) {
	response, err := access.FetchStationRealTimeData()
	if err != nil {
		logging.Fatal("failed to fetch station real time data", "error", err)
	}
	if err := access.SaveStationRealKpiResponse(response); err != nil {
		logging.Fatal("failed to save station real kpi response", "error", err)
	}
}

func runDeviceRealKpi(access *fusionsolar.FusionSolarAccess) {
	response, err := access.FetchDeviceRealData()
	if err != nil {
		logging.Fatal("failed to fetch device real data", "error", err)
	}
	if err := access.SaveDeviceRealKpiResponse(response); err != nil {
		logging.Fatal("failed to save device real kpi response", "error", err)
	}
}

func runStationDayKpi(access *fusionsolar.FusionSolarAccess) {
	response, err := access.FetchStationDayKpi()
	if err != nil {
		logging.Fatal("failed to fetch station day kpi", "error", err)
	}
	if err := access.SaveStationDayKpiResponse(response); err != nil {
		logging.Fatal("failed to save station day kpi response", "error", err)
	}
}

func runStationMonthKpi(access *fusionsolar.FusionSolarAccess) {
	response, err := access.FetchMonthData()
	if err != nil {
		logging.Fatal("failed to fetch station month kpi", "error", err)
	}
	if err := access.SaveStationMonthKpiResponse(response); err != nil {
		logging.Fatal("failed to save station month kpi response", "error", err)
	}
}

func runStationYearKpi(access *fusionsolar.FusionSolarAccess) {
	response, err := access.FetchYearData()
	if err != nil {
		logging.Fatal("failed to fetch station year kpi", "error", err)
	}
	if err := access.SaveStationYearKpiResponse(response); err != nil {
		logging.Fatal("failed to save station year kpi response", "error", err)
	}
}

func runDevice5MinKpi(access *fusionsolar.FusionSolarAccess) {
	if err := access.CollectDevFiveMinutesKpis(); err != nil {
		logging.Fatal("failed to collect device 5 minutes kpi", "error", err)
	}
}
