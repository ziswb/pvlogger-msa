package config

import (
	"os"
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

func getIntEnv(key, fallback string) int {
	value, err := strconv.Atoi(getEnv(key, fallback))
	if err != nil {
		panic(err)
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
			panic(err)
		}
		result = append(result, uint8(safeId))
	}
	return result
}
