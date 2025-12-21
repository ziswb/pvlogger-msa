package services

import (
	"encoding/binary"
	"fmt"
	"io"
	"log"
	"net"
	"pvlogger-modbus-data-collector-go/app/internal/config"
	"time"
)

type TcpHandler struct {
}

func NewTcpHandler() *TcpHandler {
	return &TcpHandler{}
}

func writeFull(w io.Writer, b []byte) error {
	for len(b) > 0 {
		n, err := w.Write(b)
		if err != nil {
			return err
		}
		b = b[n:]
	}
	return nil
}

func (handler *TcpHandler) WriteRequest(request *ReadHoldingRegistersValueRequest) {
	url := fmt.Sprintf("%s:%d", config.ModbusConfig().Host, config.ModbusConfig().Port)
	conn, err := (&net.Dialer{Timeout: 5 * time.Second, KeepAlive: 30 * time.Second}).Dial("tcp", url)
	if err != nil {
		log.Default().Printf("Error connecting to Modbus device: %v", err)
		return
	}

	sock := conn.(*net.TCPConn)
	_ = sock.SetKeepAlive(true)
	_ = sock.SetNoDelay(true) // reduces latency/coalescing

	time.Sleep(1 * time.Second) // matches what you discovered

	//sock, err := net.DialTimeout("tcp", url, 5*time.Second)
	//if err != nil {
	//	log.Default().Printf("Error connecting to Modbus device: %v", err)
	//	return
	//}
	defer func(sock net.Conn) {
		log.Default().Println("Closing Modbus connection...")
		err := sock.Close()
		if err != nil {
			log.Default().Printf("Error closing Modbus connection: %v", err)
		}
	}(sock)
	//err = sock.SetDeadline(time.Now().Add(15 * time.Second))
	//if err != nil {
	//	log.Default().Printf("Error setting Modbus connection deadline: %v", err)
	//	return
	//}

	bytes := request.EncodeToBytes()

	log.Default().Println("#bytes: ", len(bytes))
	log.Default().Println("Bytes: ", bytes)

	err = conn.SetWriteDeadline(time.Now().Add(1 * time.Second))
	if err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return
	}
	if err := writeFull(conn, bytes); err != nil { /* handle */
		log.Default().Printf("Error writing Modbus frame: %v", err)
		return
	}

	err = conn.SetReadDeadline(time.Now().Add(5 * time.Second))
	if err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return
	}

	//time.Sleep(1 * time.Second) // matches what you discovered

	mbap := make([]byte, 7)
	if _, err := io.ReadFull(conn, mbap); err != nil { /* handle */
		log.Default().Printf("Error reading header: %v", err)
		return
	}
	log.Default().Println("MBAP: ", mbap)
	// mbap[4:6] = length (UnitID + PDU)
	length := int(binary.BigEndian.Uint16(mbap[4:6]))
	remaining := length - 1
	rest := make([]byte, remaining)
	if _, err := io.ReadFull(conn, rest); err != nil { /* handle */
		log.Default().Printf("Error reading rest: %v", err)
		return
	}
	log.Default().Println("Rest: ", rest)

	// full response = append(mbap, rest...)

	//write, err := sock.Write(bytes)
	//if err != nil {
	//	return
	//}
	//if write != len(bytes) {
	//	log.Default().Printf("Error writing Modbus frame: %v", err)
	//	return
	//}
	//headerBytes := make([]byte, 7)
	//log.Default().Println("Reading Modbus frame...")
	//_, err = io.ReadFull(sock, headerBytes)
	//if err != nil {
	//	log.Default().Printf("Error reading Modbus frame: %v", err)
	//	return
	//}
	//mbapHeader := modbus.NewMbapHeaderFromBytes(headerBytes)
	//log.Default().Printf("MBAP Header: %v", mbapHeader)
	//fcBytes := make([]byte, 1)
	//_, err = sock.Read(fcBytes)
	//if err != nil {
	//	log.Default().Printf("Error reading Modbus frame: %v", err)
	//	return
	//}
	//functionCode := modbus.NewFunctionCodeFromBytes(fcBytes)
	//log.Default().Printf("Function Code: %v", functionCode)
	//numberOfBytes := make([]byte, 1)
	//_, err = sock.Read(numberOfBytes)
	//if err != nil {
	//	log.Default().Printf("Error reading Modbus frame: %v", err)
	//	return
	//}
	//log.Default().Printf("Number of Bytes: %v", numberOfBytes)
	//dataBytes := make([]byte, numberOfBytes[0])
	//_, err = sock.Read(dataBytes)
	//if err != nil {
	//	log.Default().Printf("Error reading Modbus frame: %v", err)
	//	return
	//}
	//log.Default().Printf("Data Bytes: %v", dataBytes)
	log.Default().Printf("---------------------------------------")
	log.Default().Printf("")
}
