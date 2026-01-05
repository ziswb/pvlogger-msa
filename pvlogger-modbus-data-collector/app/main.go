package main

import (
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-modbus-data-collector/internal/logging"
	"pvlogger-modbus-data-collector/internal/services"
)

type JobNameEnum = string

const (
	FetchModbusDynamicDataJob JobNameEnum = "FETCH_MODBUS_DYNAMIC_DATA_JOB"
	FetchModbusStaticDataJob  JobNameEnum = "FETCH_MODBUS_STATIC_DATA_JOB"
	FetchDeviceIdentifiersJob JobNameEnum = "FETCH_DEVICE_IDENTIFIERS_JOB"
	FetchDeviceListJob        JobNameEnum = "FETCH_DEVICE_LIST_JOB"
)

func main() {
	logging.Info("starting")
	jobName := config.JobConfig().JobName
	switch jobName {
	case FetchModbusDynamicDataJob:
		{
			jobId := services.FetchModbusDynamicRegisterValues()
			services.AggregateKpisFor(jobId)
		}
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
