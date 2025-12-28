package model

type UByte = uint8
type UShort = uint16
type Short = int16
type UInt = uint32
type DataType = string
type ProtocolType = UShort

type RegisterType int

const (
	RegisterTypeU16 RegisterType = iota
	RegisterTypeU32
	RegisterTypeI16
	RegisterTypeI32
	RegisterTypeSTR
	RegisterTypeMLD
	RegisterTypeBitfield16
	RegisterTypeBitfield32
)

var registerTypeName = map[RegisterType]string{
	RegisterTypeU16:        "U16",
	RegisterTypeU32:        "U32",
	RegisterTypeI16:        "I16",
	RegisterTypeI32:        "I32",
	RegisterTypeSTR:        "STR",
	RegisterTypeMLD:        "MLD",
	RegisterTypeBitfield16: "BITFIELD_16",
	RegisterTypeBitfield32: "BITFIELD_32",
}

var registerTypeEnum = map[string]RegisterType{
	"U16":         RegisterTypeU16,
	"U32":         RegisterTypeU32,
	"I16":         RegisterTypeI16,
	"I32":         RegisterTypeI32,
	"STR":         RegisterTypeSTR,
	"MLD":         RegisterTypeMLD,
	"BITFIELD_16": RegisterTypeBitfield16,
	"BITFIELD_32": RegisterTypeBitfield32,
}

func (r RegisterType) ToString() string {
	return registerTypeName[r]
}

func ToRegisterType(s string) RegisterType {
	return registerTypeEnum[s]
}
