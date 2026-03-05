package main

import (
	"pv-fast-data-collector/internal/services"
	"pv-logger/logging"
	"time"
)

func main() {
	now := time.Now()
	if now.Hour() == 0 && now.Minute() < 5 {
		logging.Info("skipping pv-fast-data-collector: within daily pause window (00:00-00:04)")
		return
	}
	logging.Info("starting pv-fast-data-collector")
	services.CollectAndComputeKpis()
	logging.Info("finished pv-fast-data-collector")
}
