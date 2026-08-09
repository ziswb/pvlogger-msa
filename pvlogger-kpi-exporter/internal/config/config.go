package config

import (
	env "pv-logger/config"
)

type Config struct {
	ExportPath   string
	FilePrefix   string
	RestHost     string
	RestPort     string
	RestPath     string
	RestProtocol string
}

func Load() *Config {
	return &Config{
		ExportPath:   env.GetEnvChecked("KPI_EXPORT_PATH"),
		FilePrefix:   env.GetEnvChecked("KPI_FILE_PREFIX"),
		RestHost:     env.GetEnv("REST_HOST", "localhost"),
		RestPort:     env.GetEnv("REST_PORT", "8083"),
		RestPath:     env.GetEnv("REST_PATH", "/v1/read-latest-kpi"),
		RestProtocol: env.GetEnv("REST_PROTOCOL", "http"),
	}
}
