package config

type ModbusJobConfig struct {
	JobName string
}

func JobConfig() ModbusJobConfig {
	return ModbusJobConfig{
		JobName: getEnv("JOB_NAME", "FETCH_MODBUS_DYNAMIC_DATA_JOB"),
	}
}
