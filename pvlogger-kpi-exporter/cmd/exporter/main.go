package main

import (
	"pvlogger-kpi-exporter/internal/config"
	"pvlogger-kpi-exporter/internal/datetime"
	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
)

func main() {
	cfg := config.Load()

	kpis := kpi.FetchKpis(cfg)
	record := kpi.MapToExportRecord(kpis)
	file.WriteJSON(&record, cfg.ExportPath, cfg.FilePrefix+"-"+datetime.FormatFileNameSafe(kpis.CreatedAt))
	file.Cleanup(cfg.ExportPath)
}
