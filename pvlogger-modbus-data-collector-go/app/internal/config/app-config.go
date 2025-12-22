package config

import (
	"os"
	"strconv"
	"strings"
	"time"
)

type MasterDataConfig struct {
	Host           string
	Protocol       string
	Port           int
	MasterDataPath string
}

type DeviceIdConfig struct {
	MeterDeviceIds    []uint8
	InverterDeviceIds []uint8
}

type ModbusCfg struct {
	Host    string
	Port    int
	Timeout time.Duration
}

func MdConfig() *MasterDataConfig {
	return &MasterDataConfig{
		Host:           getEnv("MD_HOST", "localhost"),
		Port:           getIntEnv("MD_PORT", "8081"),
		Protocol:       getEnv("MD_PROTOCOL", "http"),
		MasterDataPath: getEnv("MD_PATH", "/master-data"),
	}
}

func DevIdConfig() DeviceIdConfig {
	return DeviceIdConfig{
		MeterDeviceIds:    getUbyteArrayEnv("METER_DEVICE_IDS", "1"),
		InverterDeviceIds: getUbyteArrayEnv("INVERTER_DEVICE_IDS", "1,16"),
	}
}

func ModbusConfig() ModbusCfg {
	return ModbusCfg{
		Host:    getEnv("MODBUS_HOSTNAME", "<dongle-ip>"),
		Port:    getIntEnv("MODBUS_PORT", "502"),
		Timeout: time.Duration(getIntEnv("MODBUS_TIMEOUT", "5")) * time.Second,
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

func getUbyteArrayEnv(key, fallback string) []uint8 {
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
