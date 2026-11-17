package kpi

import (
	"bytes"
	"encoding/json"

	"pvlogger-kpi-exporter/internal/datetime"
)

type ExportRecord struct {
	LastUpdateTime string               `json:"lastUpdateTime"`
	CurrentData    CurrentDataRecord    `json:"currentData"`
	CurrentDayData CurrentDayDataRecord `json:"currentDayData"`
}

// The value keys are "<keyPrefix>_<name>": the prefix is the receiver's and part
// of its contract (ADR-0021), so it is configuration (KPI_KEY_PREFIX), not code.

type CurrentDataRecord struct {
	ProdAktW  float64
	VerkAktW  float64
	KaufAktW  float64
	VerbAktW  float64
	keyPrefix string
}

type CurrentDayDataRecord struct {
	ProdTagWh float64
	VerkTagWh float64
	KaufTagWh float64
	VerbTagWh float64
	keyPrefix string
}

func (r CurrentDataRecord) MarshalJSON() ([]byte, error) {
	return prefixedObject(r.keyPrefix, []field{
		{"prod_akt_w", r.ProdAktW}, {"verk_akt_w", r.VerkAktW}, {"kauf_akt_w", r.KaufAktW}, {"verb_akt_w", r.VerbAktW},
	})
}

func (r CurrentDayDataRecord) MarshalJSON() ([]byte, error) {
	return prefixedObject(r.keyPrefix, []field{
		{"prod_tag_wh", r.ProdTagWh}, {"verk_tag_wh", r.VerkTagWh}, {"kauf_tag_wh", r.KaufTagWh}, {"verb_tag_wh", r.VerbTagWh},
	})
}

type field struct {
	name  string
	value float64
}

// prefixedObject writes the fields in the given order, as the struct tags did.
func prefixedObject(prefix string, fields []field) ([]byte, error) {
	var buf bytes.Buffer
	buf.WriteByte('{')
	for i, f := range fields {
		if i > 0 {
			buf.WriteByte(',')
		}
		key, err := json.Marshal(prefix + "_" + f.name)
		if err != nil {
			return nil, err
		}
		value, err := json.Marshal(f.value)
		if err != nil {
			return nil, err
		}
		buf.Write(key)
		buf.WriteByte(':')
		buf.Write(value)
	}
	buf.WriteByte('}')
	return buf.Bytes(), nil
}

// FileName is today's export file name without the .json extension:
// <prefix>-YYYY-MM-DD_HH-MM-SS.mmm in local time.
func FileName(prefix string, createdAt int64) string {
	return prefix + "-" + datetime.FormatFileNameSafe(createdAt)
}

// MapToExportRecord maps the KPIs to the receiver's record; keyPrefix starts
// every value key.
func MapToExportRecord(keyPrefix string, kpis *Values) ExportRecord {
	return ExportRecord{
		LastUpdateTime: datetime.FormatForDataRecord(kpis.CreatedAt),
		CurrentData: CurrentDataRecord{
			keyPrefix: keyPrefix,
			ProdAktW:  kpis.SolarPower * 1000,
			VerkAktW:  kpis.SolarToGridPower * 1000,
			KaufAktW:  kpis.GridToHousePower * 1000,
			// Measured consumption (register 37500), the dashboard's "Energieverbrauch".
			// Until 2026-10-20 this was export + import, which read 0 W while the
			// house ran on solar alone.
			VerbAktW: kpis.OwnConsumption * 1000,
		},
		CurrentDayData: CurrentDayDataRecord{
			keyPrefix: keyPrefix,
			ProdTagWh: kpis.DailyEnergyYield * 1000,
			VerkTagWh: kpis.DailyEnergyExported * 1000,
			KaufTagWh: kpis.DailyEnergyImported * 1000,
			VerbTagWh: kpis.DailyEnergyConsumed * 1000,
		},
	}
}
