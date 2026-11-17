package persistence

// RawKpiRegisterValues is the data accessor's raw KPI row (POST
// store-raw-kpi-register-values): the registers the day-difference SQL reads.
// One type for the fast collector and the history backfill; its JSON names
// are pinned to the Kotlin class by raw_kpi_drift_test.go.
type RawKpiRegisterValues struct {
	JobId                    string  `json:"jobId"`
	InverterActivePower      float64 `json:"inverterActivePower"` // kW
	MeterActivePower         float64 `json:"meterActivePower"`    // W, positive = export
	InverterDailyYield       float64 `json:"inverterDailyYield"`
	InverterAccumulatedYield float64 `json:"inverterAccumulatedYield"`
	MeterExportedEnergy      float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy      float64 `json:"meterImportedEnergy"`
	// DateCreated (epoch ms) is for the dev backfill only: the data accessor
	// refuses it unless it is a dev build (ADR-0025), so it must stay
	// absent (nil) for the collectors.
	DateCreated *int64 `json:"dateCreated,omitempty"`
}
