package config

import (
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
		Host:            getEnv("MODBUS_HOSTNAME", "<dongle-ip>"),
		Port:            getIntEnv("MODBUS_PORT", "502"),
		Timeout:         time.Duration(getIntEnv("MODBUS_TIMEOUT", "5")) * time.Second,
		ConnectionSleep: time.Duration(getIntEnv("MODBUS_CONNECTION_SLEEP_MS", "1000")) * time.Millisecond,
	}
}
