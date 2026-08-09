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
	var err error
	switch jobName {
	case FetchModbusStaticDataJob:
		err = services.FetchModbusStaticRegisterValues()
	case FetchDeviceIdentifiersJob:
		err = services.FetchDeviceIdentifiers()
	case FetchDeviceListJob:
		err = services.FetchDeviceList()
	default:
		logging.Fatal("unknown job name", "job_name", jobName)
	}
	if err != nil {
		logging.Fatal("job failed", "job_name", jobName, "error", err)
	}
	logging.Info("finished")
}
