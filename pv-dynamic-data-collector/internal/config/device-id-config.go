package config

import "pv-dynamic-data-collector/internal/modbus/model"

type DeviceIdConfig struct {
	MeterDeviceIds    []model.UByte
	InverterDeviceIds []model.UByte
}

func DevIdConfig() DeviceIdConfig {
	return DeviceIdConfig{
		MeterDeviceIds:    getUByteArrayEnv("METER_DEVICE_IDS", "1"),
		InverterDeviceIds: getUByteArrayEnv("INVERTER_DEVICE_IDS", "1,16"),
	}
}
