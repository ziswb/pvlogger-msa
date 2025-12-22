package modbus

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
	return bytes
}

func NewFunctionCodeFromByte(b byte) FunctionCode {
	return FunctionCode(b)

}
