package main

import (
	"pv-full-data-collector/internal/services"
	"pv-logger/logging"
)

func main() {
	logging.Info("starting pv-full-data-collector")
	if err := services.FetchDynamicDataAndAggregateKpis(); err != nil {
		logging.Fatal("pv-full-data-collector failed", "error", err)
	}
	logging.Info("finished pv-full-data-collector")
}
