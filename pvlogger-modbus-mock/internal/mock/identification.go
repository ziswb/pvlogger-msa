package mock

import "fmt"

// object is one device identification object.
type object struct {
	id    byte
	value []byte
}

const (
	idCodeBasic      = 0x01
	idCodeDeviceList = 0x03
	objDeviceCount   = 0x87
	softwareVersion  = "V100R001C00SPC100"
)

// identification returns the objects for a read device identification
// request, or nil if the request asks for something the dongle does not
// serve. The device list follows Huawei's description format: one object per
// device, "1=model;2=software;3=protocol;4=serial;5=unit;6=feature".
func identification(code, objectID, unit byte) []object {
	switch {
	case code == idCodeBasic && objectID == 0x00:
		return []object{
			{0x00, []byte("HUAWEI")},
			{0x01, []byte("SUN2000")},
			{0x02, []byte(softwareVersion)},
		}
	case code == idCodeDeviceList && objectID == objDeviceCount:
		description := fmt.Sprintf("1=%s;2=%s;3=P1.0-D5.0;4=%s;5=%d;6=0;7=0;8=0",
			inverterModel, softwareVersion, inverterSerial, unit)
		return []object{
			{objDeviceCount, []byte{1}},
			{objDeviceCount + 1, []byte(description)},
		}
	}
	return nil
}
