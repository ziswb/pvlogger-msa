package main

import (
	"pv-fast-data-collector/internal/services"
	"pv-logger/logging"
	"time"
)

func main() {
	now := time.Now()
	if now.Hour() == 0 && (now.Minute() == 0 || now.Minute() > 1 && now.Minute() < 5) {
		logging.Info("skipping pv-fast-data-collector: within daily pause window (00:00 and 00:02-00:04)")
		return
	}
	logging.Info("starting pv-fast-data-collector")
	if err := services.CollectAndComputeKpis(); err != nil {
		logging.Fatal("pv-fast-data-collector failed", "error", err)
	}
	logging.Info("finished pv-fast-data-collector")
}
