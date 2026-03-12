package config

import (
	"pv-logger/config"
	"time"
)

type ModbusCfg struct {
	Host            string
	Port            int
	Timeout         time.Duration
	ConnectionSleep time.Duration
}

func ModbusConfig() ModbusCfg {
	return ModbusCfg{
		Host:            config.GetEnvChecked("MODBUS_HOSTNAME"),
		Port:            config.GetIntEnv("MODBUS_PORT", "502"),
		Timeout:         time.Duration(config.GetIntEnv("MODBUS_TIMEOUT", "5")) * time.Second,
		ConnectionSleep: time.Duration(config.GetIntEnv("MODBUS_CONNECTION_SLEEP_MS", "1000")) * time.Millisecond,
	}
}
