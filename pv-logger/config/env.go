package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

// The helpers in this package are Must-style: missing required variables or
// unparseable values panic. They are meant for startup configuration only,
// where aborting the process is the single sane outcome (ADR-0010/0011).

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
		panic("missing required environment variable: " + key)
	}
	return value
}

// GetEnvOptional returns the variable's value, panicking only when it is
// missing and optional is false.
func GetEnvOptional(key string, optional bool) string {
	value := os.Getenv(key)
	if len(value) == 0 && !optional {
		panic("missing required environment variable: " + key)
	}
	return value
}

func GetIntEnv(key, fallback string) int {
	value, err := strconv.Atoi(GetEnv(key, fallback))
	if err != nil {
		panic(fmt.Sprintf("invalid int environment variable %s: %v", key, err))
	}
	return value
}

func GetBoolEnv(key, fallback string) bool {
	value, err := strconv.ParseBool(GetEnv(key, fallback))
	if err != nil {
		panic(fmt.Sprintf("invalid bool environment variable %s: %v", key, err))
	}
	return value
}

func GetUByteArrayEnv(key, fallback string) []uint8 {
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
			panic(fmt.Sprintf("invalid ubyte array environment variable %s: %v", key, err))
		}
		result = append(result, uint8(safeId))
	}
	return result
}
