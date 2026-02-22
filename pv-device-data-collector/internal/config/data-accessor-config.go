package config

type DataAccessorConfig struct {
	Host                    string
	Port                    int
	Protocol                string
	BasePath                string
	StoreModbusValuesPath   string
	StoreDeviceIdValuesPath string
}

func DataAccessorCfg() *DataAccessorConfig {
	return &DataAccessorConfig{
		Host:                    getEnv("DA_HOST", "localhost"),
		Port:                    getIntEnv("DA_PORT", "8083"),
		Protocol:                getEnv("DA_PROTOCOL", "http"),
		BasePath:                getEnv("DA_BASE_PATH", "v1"),
		StoreModbusValuesPath:   getEnv("DA_STORE_MODBUS_VALUES_PATH", "store-modbus-values"),
		StoreDeviceIdValuesPath: getEnv("DA_STORE_DEVICE_ID_VALUES_PATH", "store-device-id-values"),
	}
}
