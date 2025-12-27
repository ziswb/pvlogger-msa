package main

import (
	"fmt"
	"log"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/services"
)

type JobNameEnum = string

const (
	FetchModbusDynamicDataJob JobNameEnum = "FETCH_MODBUS_DYNAMIC_DATA_JOB"
	FetchModbusStaticDataJob  JobNameEnum = "FETCH_MODBUS_STATIC_DATA_JOB"
)

func main() {
	log.Default().Println("Starting ...")
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
	default:
		log.Default().Println(fmt.Sprintf("Unknown job name: '%s'", jobName))
	}
	log.Default().Println("Finished")
}
