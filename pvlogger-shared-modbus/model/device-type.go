package model

type DeviceType string

const (
	InverterDeviceType DeviceType = "INVERTER"
	MeterDeviceType    DeviceType = "METER"
	// DongleDeviceType is the SmartDongle's own logical Modbus device (unit
	// 100, see DONGLE_DEVICE_ID): it serves the aggregate live-power block
	// 37498 x20 the KPIs are computed from (ADR-0016, backlog items 33/35).
	DongleDeviceType DeviceType = "DONGLE"
)

func (dt DeviceType) String() string {
	return string(dt)
}

// Int returns the Huawei FusionSolar/SmartPVMS device type id (used by
// pvlogger-fusionsolar's API requests), not the Modbus unit id.
func (dt DeviceType) Int() int {
	switch dt {
	case InverterDeviceType:
		return 1
	case MeterDeviceType:
		return 47
	case DongleDeviceType:
		return 62
	default:
		return -1
	}
}
