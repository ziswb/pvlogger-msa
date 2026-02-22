package main

import (
	"pv-dynamic-data-collector/internal/services"
	"pvlogger-go-shared/logging"
)

func main() {
	logging.Info("starting pv-dynamic-data-collector")
	services.FetchDynamicDataAndAggregateKpis()
	logging.Info("finished pv-dynamic-data-collector")
}
