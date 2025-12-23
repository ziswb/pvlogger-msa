package main

import (
	"log"
	"pvlogger-modbus-data-collector-go/internal/services"
)

func main() {
	log.Default().Println("Starting ...")
	jobId := services.FetchModbusDynamicRegisterValues()
	kpis := services.AggregateKpisFor(jobId)
	log.Default().Println("TODO: Store KPIs:", kpis)
	// TODO: store KPIs
	log.Default().Println("Finished")
}
