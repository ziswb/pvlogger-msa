package main

import (
	"pv-quick-data-collector/internal/services"
	"pvlogger-go-shared/logging"
)

func main() {
	logging.Info("starting pv-quick-data-collector")
	services.CollectAndComputeKpis()
	logging.Info("finished pv-quick-data-collector")
}
