package modbus

var ReadRegisters = NewFunctionCode(0x03)
var WriteSingleRegister = NewFunctionCode(0x06)
var WriteMultipleRegisters = NewFunctionCode(0x10)
var ReadDeviceIdentifiers = NewFunctionCode(0x28)
var AbnormalResponse = NewFunctionCode(0x83)
var UnknownResponse = NewFunctionCode(0x0)

type FunctionCode struct {
	Value UByte `json:"value"`
}

func NewFunctionCode(value UByte) FunctionCode {
	return FunctionCode{Value: value}
}

func (f *FunctionCode) EncodeToBytes() []byte {
	bytes := []byte{f.Value}
	return bytes
}

func NewFunctionCodeFromByte(b byte) FunctionCode {
	return FunctionCode{Value: b}

}
