package model

type Category string

const (
	StaticCategory  Category = "STATIC"
	DynamicCategory Category = "DYNAMIC"
	ConfigCategory  Category = "CONFIG"
	AlarmCategory   Category = "ALARM"
	DevListCategory Category = "DEVLIST"
)

func (c Category) String() string {
	return string(c)
}
