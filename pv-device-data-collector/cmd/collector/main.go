package main

import (
	"pv-logger/logging"
	"pv-device-data-collector/internal/config"
	"pv-device-data-collector/internal/services"
)

type JobNameEnum = string

const (
	FetchModbusStaticDataJob  JobNameEnum = "FETCH_MODBUS_STATIC_DATA_JOB"
	FetchDeviceIdentifiersJob JobNameEnum = "FETCH_DEVICE_IDENTIFIERS_JOB"
	FetchDeviceListJob        JobNameEnum = "FETCH_DEVICE_LIST_JOB"
)

func main() {
	logging.Info("starting")
	jobName := config.JobConfig().JobName
	switch jobName {
	case FetchModbusStaticDataJob:
		{
			services.FetchModbusStaticRegisterValues()
		}
	case FetchDeviceIdentifiersJob:
		{
			services.FetchDeviceIdentifiers()
		}
	case FetchDeviceListJob:
		{
			services.FetchDeviceList()
		}
	default:
		logging.Fatal("unknown job name", "job_name", jobName)
	}
	logging.Info("finished")
}
