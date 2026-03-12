package config

import (
	"os"
	"pv-logger/logging"
	"strconv"
	"strings"
)

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		return fallback
	}
	return value
}

func getEnvChecked(key string) string {
	value := os.Getenv(key)
	if len(value) == 0 {
		logging.Fatal("missing required environment variable", "key", key)
	}
	return value
}

func getIntEnv(key, fallback string) int {
	value, err := strconv.Atoi(getEnv(key, fallback))
	if err != nil {
		logging.Fatal("error getting value for int environment variable", "key", key, "error", err)
	}
	return value
}

func getUByteArrayEnv(key, fallback string) []uint8 {
	value := os.Getenv(key)
	var parts []string
	if len(value) == 0 {
		parts = strings.Split(fallback, ",")
	} else {
		parts = strings.Split(value, ",")
	}
	var result []uint8
	for _, id := range parts {
		safeId, err := strconv.Atoi(id)
		if err != nil {
			logging.Fatal("error getting value for ubyte array environment variable", "key", key, "error", err)
		}
		result = append(result, uint8(safeId))
	}
	return result
}
