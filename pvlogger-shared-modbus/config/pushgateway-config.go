package config

import "pv-logger/config"

type PushGatewayConfig struct {
	Host     string
	Port     int
	Protocol string
	Enabled  bool
}

func PushGatewayCfg() *PushGatewayConfig {
	return &PushGatewayConfig{
		Host:     config.GetEnv("PUSHGATEWAY_HOST", "localhost"),
		Port:     config.GetIntEnv("PUSHGATEWAY_PORT", "9091"),
		Protocol: config.GetEnv("PUSHGATEWAY_PROTOCOL", "http"),
		Enabled:  config.GetBoolEnv("PUSHGATEWAY_ENABLED", "false"),
	}
}
