package config

import cfg "pv-logger/config"

// REST paths of the data-accessor API. These are API contract, not deployment
// config, so they are deliberately not env-configurable (ADR-0012). Source of
// truth are the Kotlin route constants in pvlogger-shared-rest
// (DataAccessorPaths.kt); paths_drift_test.go fails when they drift.
const (
	DataAccessorBasePath          = "v1"
	StoreModbusValuesPath         = "store-modbus-values"
	StoreKpiValuesPath            = "store-kpi-values"
	StoreDeviceIdValuesPath       = "store-device-id-values"
	StoreRawKpiRegisterValuesPath = "store-raw-kpi-register-values"
	ReadLatestRegisterValuePath   = "read-latest-register-value"
	ReadMeterEnergyTodayPath      = "meter-energy-today"
	ReadDayDataPath               = "daydata"
)

// DataAccessorConfig holds where the data accessor runs — the only part of
// reaching it that is deployment-specific.
type DataAccessorConfig struct {
	Host     string
	Port     int
	Protocol string
}

func DataAccessorCfg() *DataAccessorConfig {
	return &DataAccessorConfig{
		Host:     cfg.GetEnv("DA_HOST", "localhost"),
		Port:     cfg.GetIntEnv("DA_PORT", "8083"),
		Protocol: cfg.GetEnv("DA_PROTOCOL", "http"),
	}
}
