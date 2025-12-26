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
		return NewKW(value).ToW()
	case UnitW:
		return NewW(value)
	default:
		{
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to W")
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			return NewW(0.0)
		}
	}
}

func (u *Unit) toKW(value float64) KW {
	switch u.unitId {
	case UnitKW:
		return NewKW(value)
	case UnitW:
		return NewW(value).ToKW()
	default:
		{
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to kW")
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			return NewKW(0.0)
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
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			log.Default().Println("unit is", string(u.unitId), "and cannot be converted to kWh")
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
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
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			log.Default().Println("Invalid unit: " + text)
			log.Default().Println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
			return Unit{UnitW}
		}
	}
}

type Unit struct {
	unitId UnitId
}
