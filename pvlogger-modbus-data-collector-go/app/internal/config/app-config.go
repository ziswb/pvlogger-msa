package config

import (
	"os"
	"strconv"
)

type MasterDataConfig struct {
	Host           string
	Protocol       string
	Port           int
	MasterDataPath string
}

func MdConfig() MasterDataConfig {
	return MasterDataConfig{
		Host:           getEnv("MD_HOST", "localhost"),
		Port:           getIntEnv("MD_PORT", "8081"),
		Protocol:       getEnv("MD_PROTOCOL", "http"),
		MasterDataPath: getEnv("MD_PATH", "/master-data"),
	}
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		return fallback
	}
	return value
}

func getIntEnv(key, fallback string) int {
	value, err := strconv.Atoi(getEnv(key, fallback))
	if err != nil {
		panic(err)
	}
	return value
}
