package model

import "strings"

type FusionSolarDataType int

const (
	StationRealKpi FusionSolarDataType = iota
	StationDayKpi
	StationMonthKpi
	StationYearKpi
	DeviceRealKpi
	Device5MinKpi
)

var fsDataTypeNames = [...]string{
	StationRealKpi:  "STATION_REAL_KPI",
	StationDayKpi:   "STATION_DAY_KPI",
	StationMonthKpi: "STATION_MONTH_KPI",
	StationYearKpi:  "STATION_YEAR_KPI",
	DeviceRealKpi:   "DEVICE_REAL_KPI",
	Device5MinKpi:   "DEVICE_5_MIN_KPI",
}

func (fsDataType FusionSolarDataType) String() string {
	return fsDataTypeNames[fsDataType]
}

func ParseString(name string) FusionSolarDataType {
	for i, name := range fsDataTypeNames {
		if name == strings.ToUpper(name) {
			return FusionSolarDataType(i)
		}
	}
	return -1
}
