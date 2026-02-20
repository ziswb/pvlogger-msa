package model

var ReadRegisters = NewFunctionCode(0x03)

type FunctionCode struct {
	Value UByte `json:"value"`
}

func NewFunctionCode(value UByte) FunctionCode {
	return FunctionCode{Value: value}
}

func (f *FunctionCode) EncodeToBytes() []byte {
	return []byte{f.Value}
}

func NewFunctionCodeFromByte(b byte) FunctionCode {
	return FunctionCode{Value: b}
}
