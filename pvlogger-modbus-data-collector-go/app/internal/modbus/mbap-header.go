package modbus

import (
	"encoding/binary"
	"log"
)

type MbapHeader struct {
	ProtocolIdentifier UShort
	ProtocolType       ProtocolType
	DataLength         UShort
	LogicDeviceId      UByte
}

func NewMbapHeader(protocolIdentifier UShort, protocolType ProtocolType, dataLength UShort, logicDeviceId UByte) MbapHeader {
	return MbapHeader{protocolIdentifier, protocolType, dataLength, logicDeviceId}
}

func (header *MbapHeader) EncodeToBytes() []byte {
	bytes := make([]byte, 7)
	binary.BigEndian.PutUint16(bytes[0:], header.ProtocolIdentifier)
	binary.BigEndian.PutUint16(bytes[2:], header.ProtocolType)
	binary.BigEndian.PutUint16(bytes[4:], header.DataLength)
	bytes[6] = header.LogicDeviceId
	log.Default().Println("Header Bytes: ", len(bytes))
	return bytes
}

func NewMbapHeaderFromBytes(bytes []byte) MbapHeader {
	if len(bytes) != 7 {
		log.Default().Println("Exactly 7 bytes are necessary for creating an MBAP header (only", len(bytes), "bytes given)")
		return MbapHeader{}
	}
	protocolIdentifier := binary.BigEndian.Uint16(bytes)
	protocolType := binary.BigEndian.Uint16(bytes[2:])
	dataLength := binary.BigEndian.Uint16(bytes[4:])
	logicDeviceId := bytes[6]
	return MbapHeader{protocolIdentifier, protocolType, dataLength, logicDeviceId}
}
