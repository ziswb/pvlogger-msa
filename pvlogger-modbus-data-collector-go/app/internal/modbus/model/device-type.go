package model

type DeviceType int

const (
	InverterDeviceType DeviceType = 1
	MeterDeviceType    DeviceType = 47
)

func (dt DeviceType) Int() int {
	return int(dt)
}
func (dt DeviceType) String() string {
	switch dt {
	case InverterDeviceType:
		return "INVERTER"
	case MeterDeviceType:
		return "METER"
	default:
		return "UNKNOWN"
	}
}
