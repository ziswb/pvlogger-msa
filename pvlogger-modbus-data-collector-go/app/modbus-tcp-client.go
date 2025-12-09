package main

import (
	"fmt"
	"log"
	"time"

	"github.com/simonvetter/modbus"
)

func main() {
	var client *modbus.ModbusClient
	var err error

	// for a TCP endpoint
	// (see examples/tls_client.go for TLS usage and options)
	client, err = modbus.NewClient(&modbus.ClientConfiguration{
		URL:     "tcp://hostname-or-ip-address:502",
		Timeout: 1 * time.Second,
	})

	if err != nil {
		log.Fatal(err)
	}

	// now that the client is created and configured, attempt to connect
	err = client.Open()
	if err != nil {
		log.Fatal(err)
	}

	// read a single 16-bit holding register at address 100
	var reg16 uint16
	reg16, err = client.ReadRegister(100, modbus.HOLDING_REGISTER)
	if err != nil {
		log.Fatal(err)
	}
	// use value
	fmt.Printf("value: %v", reg16)        // as unsigned integer
	fmt.Printf("value: %v", int16(reg16)) // as a signed integer

	// read 4 consecutive 16-bit input registers starting at address 100
	var reg16s []uint16
	reg16s, err = client.ReadRegisters(100, 4, modbus.INPUT_REGISTER)
	log.Println(reg16s)

	// read the same 4 consecutive 16-bit input registers as 2 32-bit integers
	var reg32s []uint32
	reg32s, err = client.ReadUint32s(100, 2, modbus.INPUT_REGISTER)
	log.Println(reg32s)

	// read the same 4 consecutive 16-bit registers as a single 64-bit integer
	var reg64 uint64
	reg64, err = client.ReadUint64(100, modbus.INPUT_REGISTER)
	log.Println(reg64)

	// read the same 4 consecutive 16-bit registers as a slice of bytes
	var regBs []byte
	regBs, err = client.ReadBytes(100, 8, modbus.INPUT_REGISTER)
	log.Println(regBs)

	// by default, 16-bit integers are decoded as big-endian and 32/64-bit values as
	// big-endian with the high word first.
	// change the byte/word ordering of subsequent requests to little endian, with
	// the low word first (note that the second argument only affects 32/64-bit values)
	err = client.SetEncoding(modbus.LITTLE_ENDIAN, modbus.LOW_WORD_FIRST)
	if err != nil {
		log.Fatal(err)
	}

	// read the same 4 consecutive 16-bit input registers as 2 32-bit floats
	var fl32s []float32
	fl32s, err = client.ReadFloat32s(100, 2, modbus.INPUT_REGISTER)
	log.Println(fl32s)

	// write -200 to 16-bit (holding) register 100, as a signed integer
	var s int16 = -200
	err = client.WriteRegister(100, uint16(s))

	// Switch to unit ID (a.k.a. slave ID) #4
	err = client.SetUnitId(4)
	if err != nil {
		log.Fatal(err)
	}

	// write 3 floats to registers 100 to 105
	err = client.WriteFloat32s(100, []float32{
		3.14,
		1.1,
		-783.22,
	})

	// write 0x0102030405060708 to 16-bit (holding) registers 10 through 13
	// (8 bytes i.e. 4 consecutive modbus registers)
	err = client.WriteBytes(10, []byte{
		0x01, 0x02, 0x03, 0x04,
		0x05, 0x06, 0x07, 0x08,
	})

	// close the TCP connection/serial port
	err = client.Close()
	if err != nil {
		log.Fatal(err)
	}
}
