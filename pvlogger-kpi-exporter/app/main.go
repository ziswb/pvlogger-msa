package main

func main() {
	exportPath := getEnv("KPI_EXPORT_PATH", "/Users/bziswiler/Work/Private/HuaweiFusionSolar/export")
	filePrefix := getEnv("KPI_FILE_PREFIX", "pv_export_<site>-")
	kpis := fetchKpis()
	record := mapToExportRecord(kpis)
	writeKpisToJsonFile(&record, exportPath, filePrefix+"-"+formatFileNameSafe(kpis.CreatedAt))
	cleanupFiles(exportPath)
}

func mapToExportRecord(kpis *KpiValues) KpiExportRecord {
	record := KpiExportRecord{
		LastUpdateTime: formatForDataRecord(kpis.CreatedAt),
		CurrentData: CurrentDataRecord{
			ProdAktW: kpis.SolarPower * 1000,
			VerkAktW: kpis.SolarToGridPower * 1000,
			KaufAktW: kpis.GridToHousePower * 1000,
			VerbAktW: kpis.SolarToGridPower*1000 + kpis.GridToHousePower*1000,
		},
		CurrentDayData: CurrentDayDataRecord{
			ProdTagWh: kpis.DailyEnergyYield * 1000,
			VerkTagWh: kpis.DailyEnergyExported * 1000,
			KaufTagWh: kpis.DailyEnergyImported * 1000,
			VerbTagWh: kpis.DailyEnergyConsumed * 1000,
		},
	}
	return record
}
