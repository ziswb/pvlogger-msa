package config

import (
	"pv-logger/config"
	"pvlogger-shared-modbus/model"
)

type DeviceIdConfig struct {
	MeterDeviceIds    []model.UByte
	InverterDeviceIds []model.UByte
}

func DevIdConfig() DeviceIdConfig {
	return DeviceIdConfig{
		MeterDeviceIds:    config.GetUByteArrayEnv("METER_DEVICE_IDS", "1"),
		InverterDeviceIds: config.GetUByteArrayEnv("INVERTER_DEVICE_IDS", "1,16"),
	}
}
