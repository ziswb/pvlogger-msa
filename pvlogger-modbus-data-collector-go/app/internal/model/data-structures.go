package model

import "time"

const CharSet = "ISO-8859-5"

type UByte = uint8
type UShort = uint16
type UInt = uint32

type RegisterAddress struct {
	RegisterAddress int `json:"registerAddress"`
	Value           int `json:"value"`
}

type Quantity struct {
	Quantity int `json:"quantity"`
	Value    int `json:"value"`
}

type Scope struct {
	Scope string `json:"scope"`
	Value string `json:"value"`
}

type ModbusRegisterDefinition struct {
	No              int             `json:"no"`
	Name            string          `json:"name"`
	ReadWrite       string          `json:"readWrite"`
	Type            string          `json:"type"`
	Unit            string          `json:"unit"`
	Gain            string          `json:"gain"`
	RegisterAddress RegisterAddress `json:"registerAddress"`
	Quantity        Quantity        `json:"quantity"`
	Scope           Scope           `json:"scope"`
	Category        string          `json:"category"`
}

type FunctionCode = UByte

const (
	ReadRegisters          FunctionCode = 0x03
	WriteSingleRegister    FunctionCode = 0x06
	WriteMultipleRegisters FunctionCode = 0x10
	ReadDeviceIdentifiers  FunctionCode = 0x28
	AbnormalResponse       FunctionCode = 0x83
	UnknownResponse        FunctionCode = 0x0
)

type ReadRegistersValueRequest struct {
	JobId               string
	RequestTime         time.Time
	RequestIdId         UShort
	DeviceId            UByte
	RegisterDefinitions *[]ModbusRegisterDefinition
}

type Data interface {
}

type MbapHeader struct {
	protocolIdentifier UShort
	protocolType       UShort
	dataLength         UShort
	logicDeviceId      UByte
}
type AduFrame struct {
	header       MbapHeader
	functionCode FunctionCode
	data         *Data
}
