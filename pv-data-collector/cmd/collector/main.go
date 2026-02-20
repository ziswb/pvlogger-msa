package main

import (
	"pv-data-collector/internal/services"
	"pvlogger-go-shared/logging"
)

func main() {
	logging.Info("starting pv-data-collector")
	services.CollectAndComputeKpis()
	logging.Info("finished pv-data-collector")
}
