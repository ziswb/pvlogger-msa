package model

type UByte = uint8
type UShort = uint16
type Short = int16
type UInt = uint32
type ProtocolType = UShort

type RegisterType int

const (
	RegisterTypeU32 RegisterType = iota
	RegisterTypeI32
)

var registerTypeName = map[RegisterType]string{
	RegisterTypeU32: "U32",
	RegisterTypeI32: "I32",
}

var registerTypeEnum = map[string]RegisterType{
	"U32": RegisterTypeU32,
	"I32": RegisterTypeI32,
}

func (r RegisterType) ToString() string {
	return registerTypeName[r]
}

func ToRegisterType(s string) RegisterType {
	return registerTypeEnum[s]
}
