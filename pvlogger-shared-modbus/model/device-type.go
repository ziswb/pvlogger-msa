package model

type DeviceType string

const DeviceIds = "<dev-id>,<dev-id>"

const (
	InverterDeviceType DeviceType = "INVERTER"
	MeterDeviceType    DeviceType = "METER"
)

func (dt DeviceType) String() string {
	return string(dt)
}
func (dt DeviceType) Int() int {
	switch dt {
	case InverterDeviceType:
		return 1
	case MeterDeviceType:
		return 47
	default:
		return -1
	}
}
