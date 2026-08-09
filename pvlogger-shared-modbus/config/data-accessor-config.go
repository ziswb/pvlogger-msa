package config

import cfg "pv-logger/config"

// DataAccessorConfig is the union of the data-accessor endpoints the
// collectors use; each collector reads only the fields it needs. Stated once
// here so path defaults cannot drift between collectors (backlog items 8/9).
type DataAccessorConfig struct {
	Host                        string
	Port                        int
	Protocol                    string
	BasePath                    string
	StoreModbusValuesPath       string
	StoreKpiValuesPath          string
	StoreDeviceIdValuesPath     string
	StoreRawKpiRegisterValues   string
	ReadLatestRegisterValuePath string
	ReadKwTodaySoFarPath        string
	ReadMeterEnergyTodayPath    string
}

func DataAccessorCfg() *DataAccessorConfig {
	return &DataAccessorConfig{
		Host:                        cfg.GetEnv("DA_HOST", "localhost"),
		Port:                        cfg.GetIntEnv("DA_PORT", "8083"),
		Protocol:                    cfg.GetEnv("DA_PROTOCOL", "http"),
		BasePath:                    cfg.GetEnv("DA_BASE_PATH", "v1"),
		StoreModbusValuesPath:       cfg.GetEnv("DA_STORE_MODBUS_VALUES_PATH", "store-modbus-values"),
		StoreKpiValuesPath:          cfg.GetEnv("DA_STORE_KPI_VALUES_PATH", "store-kpi-values"),
		StoreDeviceIdValuesPath:     cfg.GetEnv("DA_STORE_DEVICE_ID_VALUES_PATH", "store-device-id-values"),
		StoreRawKpiRegisterValues:   cfg.GetEnv("DA_STORE_RAW_KPI_REGISTER_VALUES_PATH", "store-raw-kpi-register-values"),
		ReadLatestRegisterValuePath: cfg.GetEnv("DA_READ_LATEST_REGISTER_VALUE_PATH", "read-latest-register-value"),
		ReadKwTodaySoFarPath:        cfg.GetEnv("DA_READ_KW_TODAY_SO_FAR_PATH", "read-kw-today-so-far"),
		ReadMeterEnergyTodayPath:    cfg.GetEnv("DA_READ_METER_ENERGY_TODAY_PATH", "meter-energy-today"),
	}
}
