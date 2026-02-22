package kpis

import (
	"pvlogger-go-shared/logging"
	"strings"
)

type UnitId string

const (
	UnitW   UnitId = "W"
	UnitKW  UnitId = "kW"
	UnitWh  UnitId = "Wh"
	UnitKWh UnitId = "kWh"
)

func (u UnitId) String() string {
	return string(u)
}

func (u *Unit) toW(value float64) W {
	switch u.unitId {
	case UnitKW:
		return NewKW(value).ToW()
	case UnitW:
		return NewW(value)
	default:
		{
			logging.Warn("unit cannot be converted to W", "unit", string(u.unitId))
			return NewW(0.0)
		}
	}
}

func (u *Unit) toKWh(value float64) KWh {
	switch u.unitId {
	case UnitKWh:
		return NewKWh(value)
	case UnitWh:
		return NewWh(value).ToKWh()
	default:
		{
			logging.Warn("unit cannot be converted to kWh", "unit", string(u.unitId))
			return NewKWh(0.0)
		}
	}
}

func ParseUnitFromString(text string) Unit {
	switch strings.ToUpper(text) {
	case strings.ToUpper(string(UnitW)):
		return Unit{UnitW}
	case strings.ToUpper(string(UnitKW)):
		return Unit{UnitKW}
	case strings.ToUpper(string(UnitWh)):
		return Unit{UnitWh}
	case strings.ToUpper(string(UnitKWh)):
		return Unit{UnitKWh}
	default:
		{
			logging.Warn("invalid unit, defaulting to W", "unit", text)
			return Unit{UnitW}
		}
	}
}

type Unit struct {
	unitId UnitId
}
