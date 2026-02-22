package config

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
		Host:                        getEnv("DA_HOST", "localhost"),
		Port:                        getIntEnv("DA_PORT", "8083"),
		Protocol:                    getEnv("DA_PROTOCOL", "http"),
		BasePath:                    getEnv("DA_BASE_PATH", "v1"),
		StoreModbusValuesPath:       getEnv("DA_STORE_MODBUS_VALUES_PATH", "store-modbus-values"),
		StoreKpiValuesPath:          getEnv("DA_STORE_KPI_VALUES_PATH", "store-kpi-values"),
		StoreDeviceIdValuesPath:     getEnv("DA_STORE_DEVICE_ID_VALUES_PATH", "store-device-id-values"),
		ReadLatestRegisterValuePath: getEnv("DA_READ_LATEST_REGISTER_VALUE_PATH", "read-latest-register-value"),
		ReadKwTodaySoFarPath:        getEnv("DA_READ_KW_TODAY_SO_FAR_PATH", "read-kw-today-so-far"),
	}
}
