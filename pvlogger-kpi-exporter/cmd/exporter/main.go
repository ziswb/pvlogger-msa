package main

import (
	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/config"
	"pvlogger-kpi-exporter/internal/datetime"
	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
)

func main() {
	cfg := config.Load()

	kpis, err := kpi.FetchKpis(cfg)
	if err != nil {
		logging.Fatal("failed to fetch KPIs", "error", err)
	}
	record := kpi.MapToExportRecord(kpis)
	if err := file.WriteJSON(&record, cfg.ExportPath, cfg.FilePrefix+"-"+datetime.FormatFileNameSafe(kpis.CreatedAt)); err != nil {
		logging.Fatal("failed to write KPI export file", "error", err)
	}
	if err := file.Cleanup(cfg.ExportPath); err != nil {
		logging.Fatal("failed to clean up export directory", "error", err)
	}
}
