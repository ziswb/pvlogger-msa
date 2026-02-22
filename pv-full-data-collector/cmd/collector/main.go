package main

import (
	"pv-full-data-collector/internal/services"
	"pvlogger-go-shared/logging"
)

func main() {
	logging.Info("starting pv-full-data-collector")
	services.FetchDynamicDataAndAggregateKpis()
	logging.Info("finished pv-full-data-collector")
}
