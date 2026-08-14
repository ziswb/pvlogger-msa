package config

import (
	"pv-logger/config"
	"pvlogger-shared-modbus/model"
)

type DeviceIdConfig struct {
	MeterDeviceIds    []model.UByte
	InverterDeviceIds []model.UByte
	// DongleDeviceId is the SmartDongle's own logical device serving the
	// aggregate live-power block (backlog 33). Unit 0 times out and unit 1
	// answers illegal-data-address for it; 100 was verified live.
	DongleDeviceId model.UByte
}

func DevIdConfig() DeviceIdConfig {
	return DeviceIdConfig{
		MeterDeviceIds:    config.GetUByteArrayEnv("METER_DEVICE_IDS", "1"),
		InverterDeviceIds: config.GetUByteArrayEnv("INVERTER_DEVICE_IDS", "1,16"),
		DongleDeviceId:    model.UByte(config.GetIntEnv("DONGLE_DEVICE_ID", "100")),
	}
}
