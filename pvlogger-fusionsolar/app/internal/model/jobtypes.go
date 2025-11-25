package model

type FusionSolarDataType int

const (
	STATION_REAL_KPI FusionSolarDataType = iota
	STATION_DAY_KPI
	STATION_MONTH_KPI
	STATION_YEAR_KPI
	DEVICE_REAL_KPI
	DEVICE_5_MIN_KPI
)

var fsDataTypeNames = [...]string{
	STATION_REAL_KPI:  "STATION_REAL_KPI",
	STATION_DAY_KPI:   "STATION_DAY_KPI",
	STATION_MONTH_KPI: "STATION_MONTH_KPI",
	STATION_YEAR_KPI:  "STATION_YEAR_KPI",
	DEVICE_REAL_KPI:   "DEVICE_REAL_KPI",
	DEVICE_5_MIN_KPI:  "DEVICE_5_MIN_KPI",
}

func (fsDataType FusionSolarDataType) String() string {
	return fsDataTypeNames[fsDataType]
}
