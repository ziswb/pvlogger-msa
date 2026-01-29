package main

import (
	"pvlogger-kpi-exporter/internal/config"
	"pvlogger-kpi-exporter/internal/datetime"
	"pvlogger-kpi-exporter/internal/file"
	"pvlogger-kpi-exporter/internal/kpi"
)

func main() {
	exportPath := config.GetEnv("KPI_EXPORT_PATH", "/Users/bziswiler/Work/Private/HuaweiFusionSolar/export")
	filePrefix := config.GetEnv("KPI_FILE_PREFIX", "pv_export_<site>")

	kpis := kpi.FetchKpis()
	record := kpi.MapToExportRecord(kpis)
	file.WriteJSON(&record, exportPath, filePrefix+"-"+datetime.FormatFileNameSafe(kpis.CreatedAt))
	file.Cleanup(exportPath)
}
