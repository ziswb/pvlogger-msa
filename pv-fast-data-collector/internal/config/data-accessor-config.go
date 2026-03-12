package config

import cfg "pv-logger/config"

type DataAccessorConfig struct {
	Host                       string
	Port                       int
	Protocol                   string
	BasePath                   string
	StoreRawKpiRegisterValues  string
	StoreKpiValuesPath         string
	ReadMeterEnergyTodayPath   string
}

func DataAccessorCfg() *DataAccessorConfig {
	return &DataAccessorConfig{
		Host:                      cfg.GetEnv("DA_HOST", "localhost"),
		Port:                      cfg.GetIntEnv("DA_PORT", "8083"),
		Protocol:                  cfg.GetEnv("DA_PROTOCOL", "http"),
		BasePath:                  cfg.GetEnv("DA_BASE_PATH", "v1"),
		StoreRawKpiRegisterValues: cfg.GetEnv("DA_STORE_RAW_KPI_REGISTER_VALUES_PATH", "store-raw-kpi-register-values"),
		StoreKpiValuesPath:        cfg.GetEnv("DA_STORE_KPI_VALUES_PATH", "store-kpi-values"),
		ReadMeterEnergyTodayPath:  cfg.GetEnv("DA_READ_METER_ENERGY_TODAY_PATH", "meter-energy-today"),
	}
}
