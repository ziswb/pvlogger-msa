package kpis

import (
	"log"
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
		return NewW(value * 1000.0)
	case UnitW:
		return NewW(value)
	default:
		{
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to W")
			return NewW(0.0)
		}
	}
}

func (u *Unit) toKW(value float64) KW {
	switch u.unitId {
	case UnitKW:
		return NewKW(value)
	case UnitW:
		return NewKW(value / 1000.0)
	default:
		{
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to kW")
			return NewKW(0.0)
		}
	}
}

func (u *Unit) toKWh(value float64) KWh {
	switch u.unitId {
	case UnitKWh:
		return NewKWh(value)
	case UnitWh:
		return NewKWh(value / 1000.0)
	default:
		{
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to kWh")
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
			log.Default().Println("Invalid unit: " + text)
			return Unit{UnitW}
		}
	}
}

type Unit struct {
	unitId UnitId
}
