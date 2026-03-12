package config

import (
	"os"
	"pv-logger/logging"
)

func GetEnv(key, fallback string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		return fallback
	}
	return value
}

func GetEnvChecked(key string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		logging.Fatal("missing required environment variable", "key", key)
	}
	return value
}
