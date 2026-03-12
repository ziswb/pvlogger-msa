package config

import cfg "pv-logger/config"

type ModbusJobConfig struct {
	JobName string
}

func JobConfig() ModbusJobConfig {
	return ModbusJobConfig{
		JobName: cfg.GetEnv("JOB_NAME", "FETCH_MODBUS_DYNAMIC_DATA_JOB"),
	}
}
