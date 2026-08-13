package main

import (
	"pv-fast-data-collector/internal/services"
	"pv-logger/logging"
)

func main() {
	// No time logic here by design: the 00:02-00:04 Modbus window reserved for
	// the nightly device collectors lives in the CronJob schedules (two cron
	// expressions in the Helm chart, ADR-0015).
	logging.Info("starting pv-fast-data-collector")
	if err := services.CollectAndComputeKpis(); err != nil {
		logging.Fatal("pv-fast-data-collector failed", "error", err)
	}
	logging.Info("finished pv-fast-data-collector")
}
