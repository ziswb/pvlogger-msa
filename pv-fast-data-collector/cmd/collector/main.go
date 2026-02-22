package main

import (
	"pv-fast-data-collector/internal/services"
	"pv-logger/logging"
)

func main() {
	logging.Info("starting pv-fast-data-collector")
	services.CollectAndComputeKpis()
	logging.Info("finished pv-fast-data-collector")
}
