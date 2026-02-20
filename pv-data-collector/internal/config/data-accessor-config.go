package config

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
		Host:                      getEnv("DA_HOST", "localhost"),
		Port:                      getIntEnv("DA_PORT", "8083"),
		Protocol:                  getEnv("DA_PROTOCOL", "http"),
		BasePath:                  getEnv("DA_BASE_PATH", "v1"),
		StoreRawKpiRegisterValues: getEnv("DA_STORE_RAW_KPI_REGISTER_VALUES_PATH", "store-raw-kpi-register-values"),
		StoreKpiValuesPath:        getEnv("DA_STORE_KPI_VALUES_PATH", "store-kpi-values"),
		ReadMeterEnergyTodayPath:  getEnv("DA_READ_METER_ENERGY_TODAY_PATH", "meter-energy-today"),
	}
}
