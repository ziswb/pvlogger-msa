package config

type PushGatewayConfig struct {
	Host     string
	Port     int
	Protocol string
	Enabled  bool
}

func PushGatewayCfg() *PushGatewayConfig {
	return &PushGatewayConfig{
		Host:     getEnv("PUSHGATEWAY_HOST", "localhost"),
		Port:     getIntEnv("PUSHGATEWAY_PORT", "9091"),
		Protocol: getEnv("PUSHGATEWAY_PROTOCOL", "http"),
		Enabled:  getBoolEnv("PUSHGATEWAY_ENABLED", "false"),
	}
}
