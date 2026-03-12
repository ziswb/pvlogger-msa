package config

import cfg "pv-logger/config"

type DataAccessorConfig struct {
	Host                        string
	Port                        int
	Protocol                    string
	BasePath                    string
	StoreModbusValuesPath       string
	StoreKpiValuesPath          string
	StoreDeviceIdValuesPath     string
	ReadLatestRegisterValuePath string
	ReadKwTodaySoFarPath        string
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
		ReadLatestRegisterValuePath: cfg.GetEnv("DA_READ_LATEST_REGISTER_VALUE_PATH", "read-latest-register-value"),
		ReadKwTodaySoFarPath:        cfg.GetEnv("DA_READ_KW_TODAY_SO_FAR_PATH", "read-kw-today-so-far"),
	}
}
