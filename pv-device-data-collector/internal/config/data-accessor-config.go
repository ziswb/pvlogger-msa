package config

import cfg "pv-logger/config"

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
		Host:                    cfg.GetEnv("DA_HOST", "localhost"),
		Port:                    cfg.GetIntEnv("DA_PORT", "8083"),
		Protocol:                cfg.GetEnv("DA_PROTOCOL", "http"),
		BasePath:                cfg.GetEnv("DA_BASE_PATH", "v1"),
		StoreModbusValuesPath:   cfg.GetEnv("DA_STORE_MODBUS_VALUES_PATH", "store-modbus-values"),
		StoreDeviceIdValuesPath: cfg.GetEnv("DA_STORE_DEVICE_ID_VALUES_PATH", "store-device-id-values"),
	}
}
