package modbus

import "log"

const (
	ReadRegisters          FunctionCode = 0x03
	WriteSingleRegister    FunctionCode = 0x06
	WriteMultipleRegisters FunctionCode = 0x10
	ReadDeviceIdentifiers  FunctionCode = 0x28
	AbnormalResponse       FunctionCode = 0x83
	UnknownResponse        FunctionCode = 0x0
)

type FunctionCode UByte

func NewFunctionCode(value UByte) FunctionCode {
	return FunctionCode(value)
}

func (f *FunctionCode) EncodeToBytes() []byte {
	bytes := []byte{byte(*f)}
	log.Default().Println("Function code bytes: ", len(bytes))
	return bytes
}

func NewFunctionCodeFromBytes(bytes []byte) FunctionCode {
	if len(bytes) != 1 {
		log.Default().Println("Exactly 1 byte necessary for creating a Function Code (", len(bytes), "bytes given)")
		return UnknownResponse
	}
	return FunctionCode(bytes[0])

}
