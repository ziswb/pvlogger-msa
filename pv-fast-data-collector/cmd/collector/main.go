package main

import (
	"pv-fast-data-collector/internal/services"
	"pv-logger/logging"
	"time"
)

func main() {
	// 00:02-00:04 is reserved for the nightly device collectors, which need the
	// single-client Modbus interface to themselves (consolidation backlog 12).
	// The former 00:00 skip is gone: daily production now derives from the
	// lifetime yield counter, so the midnight sample is safe (ADR-0014).
	now := time.Now()
	if now.Hour() == 0 && now.Minute() >= 2 && now.Minute() < 5 {
		logging.Info("skipping pv-fast-data-collector: reserved Modbus window for device collectors (00:02-00:04)")
		return
	}
	logging.Info("starting pv-fast-data-collector")
	if err := services.CollectAndComputeKpis(); err != nil {
		logging.Fatal("pv-fast-data-collector failed", "error", err)
	}
	logging.Info("finished pv-fast-data-collector")
}
