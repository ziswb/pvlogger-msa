package config

type DeviceIdConfig struct {
	MeterDeviceIds    []uint8
	InverterDeviceIds []uint8
}

func DevIdConfig() DeviceIdConfig {
	return DeviceIdConfig{
		MeterDeviceIds:    getUByteArrayEnv("METER_DEVICE_IDS", "1"),
		InverterDeviceIds: getUByteArrayEnv("INVERTER_DEVICE_IDS", "1,16"),
	}
}
