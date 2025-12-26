package main

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/services"
)

func main() {
	log.Default().Println("Starting ...")
	jobId := services.FetchModbusDynamicRegisterValues()
	services.AggregateKpisFor(jobId)
	log.Default().Println("Finished")
}
