package config

import "pv-logger/config"

type MasterDataConfig struct {
	Host           string
	Protocol       string
	Port           int
	MasterDataPath string
}

func MdConfig() *MasterDataConfig {
	return &MasterDataConfig{
		Host:           config.GetEnv("MD_HOST", "localhost"),
		Port:           config.GetIntEnv("MD_PORT", "8081"),
		Protocol:       config.GetEnv("MD_PROTOCOL", "http"),
		MasterDataPath: config.GetEnv("MD_PATH", "master-data"),
	}
}
