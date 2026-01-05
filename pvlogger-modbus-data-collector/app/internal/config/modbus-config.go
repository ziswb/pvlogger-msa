package config

import (
	"time"
)

type ModbusCfg struct {
	Host    string
	Port    int
	Timeout time.Duration
}

func ModbusConfig() ModbusCfg {
	return ModbusCfg{
		Host:    getEnv("MODBUS_HOSTNAME", "<dongle-ip>"),
		Port:    getIntEnv("MODBUS_PORT", "502"),
		Timeout: time.Duration(getIntEnv("MODBUS_TIMEOUT", "5")) * time.Second,
	}
}
