package kpi

import (
	"pvlogger-kpi-exporter/internal/datetime"
)

type ExportRecord struct {
	LastUpdateTime string               `json:"lastUpdateTime"`
	CurrentData    CurrentDataRecord    `json:"currentData"`
	CurrentDayData CurrentDayDataRecord `json:"currentDayData"`
}

type CurrentDataRecord struct {
	ProdAktW float64 `json:"pv_prod_akt_w"`
	VerkAktW float64 `json:"pv_verk_akt_w"`
	KaufAktW float64 `json:"pv_kauf_akt_w"`
	VerbAktW float64 `json:"pv_verb_akt_w"`
}

type CurrentDayDataRecord struct {
	ProdTagWh float64 `json:"pv_prod_tag_wh"`
	VerkTagWh float64 `json:"pv_verk_tag_wh"`
	KaufTagWh float64 `json:"pv_kauf_tag_wh"`
	VerbTagWh float64 `json:"pv_verb_tag_wh"`
}

func MapToExportRecord(kpis *Values) ExportRecord {
	return ExportRecord{
		LastUpdateTime: datetime.FormatForDataRecord(kpis.CreatedAt),
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
}
