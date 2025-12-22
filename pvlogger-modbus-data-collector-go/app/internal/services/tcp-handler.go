package services

import (
	"encoding/binary"
	"io"
	"log"
	"net"
	"time"
)

type TcpHandler struct {
	conn         net.Conn
	address      string
	timeout      time.Duration
	sleep        time.Duration
	writeTimeout time.Duration
	readTimeout  time.Duration
}

func NewTcpHandler(url string, timeout time.Duration) *TcpHandler {
	return &TcpHandler{
		address:      url,
		timeout:      timeout,
		sleep:        1 * time.Second,
		writeTimeout: 1 * time.Second,
		readTimeout:  5 * time.Second,
	}
}

func (h *TcpHandler) Close() error {
	if h.conn == nil {
		return nil
	}
	return h.conn.Close()
}

func (h *TcpHandler) connect() error {
	if h.conn == nil {
		conn, err := (&net.Dialer{Timeout: h.timeout, KeepAlive: 30 * time.Second}).Dial("tcp", h.address)
		if err != nil {
			return err
		}
		sock := conn.(*net.TCPConn)
		err = sock.SetKeepAlive(true)
		if err != nil {
			return err
		}
		err = sock.SetNoDelay(true)
		if err != nil {
			return err
		}
		h.conn = conn
		time.Sleep(h.sleep) // huawei modbus is not ready for requests immediately after connection
	}
	return nil
}

func (h *TcpHandler) WriteRequest(request *ReadHoldingRegistersValueRequest) {

	bytes := request.EncodeToBytes()

	log.Default().Println("#bytes: ", len(bytes))
	log.Default().Println("Bytes: ", bytes)

	err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout))
	if err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return
	}
	if err := writeFull(h.conn, bytes); err != nil { /* handle */
		log.Default().Printf("Error writing Modbus frame: %v", err)
		return
	}

	err = h.conn.SetReadDeadline(time.Now().Add(h.readTimeout))
	if err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return
	}

	//time.Sleep(1 * time.Second) // matches what you discovered

	mbap := make([]byte, 7)
	if _, err := io.ReadFull(h.conn, mbap); err != nil { /* handle */
		log.Default().Printf("Error reading header: %v", err)
		return
	}
	log.Default().Println("MBAP: ", mbap)
	// mbap[4:6] = length (UnitID + PDU)
	length := int(binary.BigEndian.Uint16(mbap[4:6]))
	remaining := length - 1
	rest := make([]byte, remaining)
	if _, err := io.ReadFull(h.conn, rest); err != nil { /* handle */
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
