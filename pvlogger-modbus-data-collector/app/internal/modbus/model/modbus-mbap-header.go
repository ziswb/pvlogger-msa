package model

import (
	"encoding/binary"
	"pvlogger-go-shared/logging"
)

type MbapHeader struct {
	ProtocolIdentifier UShort       `json:"protocolIdentifier"`
	ProtocolType       ProtocolType `json:"protocolType"`
	DataLength         UShort       `json:"dataLength"`
	LogicDeviceId      UByte        `json:"logicDeviceId"`
}

func (header *MbapHeader) EncodeToBytes() []byte {
	bytes := make([]byte, 7)
	binary.BigEndian.PutUint16(bytes[0:], header.ProtocolIdentifier)
	binary.BigEndian.PutUint16(bytes[2:], header.ProtocolType)
	binary.BigEndian.PutUint16(bytes[4:], header.DataLength)
	bytes[6] = header.LogicDeviceId
	return bytes
}

func NewMbapHeaderFromBytes(bytes []byte) MbapHeader {
	if len(bytes) != 7 {
		logging.Warn("invalid byte count for MBAP header", "expected", 7, "got", len(bytes))
		return MbapHeader{}
	}
	protocolIdentifier := binary.BigEndian.Uint16(bytes)
	protocolType := binary.BigEndian.Uint16(bytes[2:])
	dataLength := binary.BigEndian.Uint16(bytes[4:])
	logicDeviceId := bytes[6]
	return MbapHeader{protocolIdentifier, protocolType, dataLength, logicDeviceId}
}
