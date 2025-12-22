package modbus

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
	log.Default().Printf("---------------------------------------")
	log.Default().Println("Closing Modbus connection ...")
	if h.conn == nil {
		return nil
	}
	log.Default().Printf("---------------------------------------")
	return h.conn.Close()
}

func (h *TcpHandler) Connect() error {
	if h.conn == nil {
		log.Default().Printf("---------------------------------------")
		log.Default().Println("Connecting to Modbus device ...")
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
		log.Default().Printf("---------------------------------------")
	}
	return nil
}

func (h *TcpHandler) FetchModbusRegisterValues(request *ReadHoldingRegistersValueRequest) (*ModbusRegisterValueResponse, error) {
	bytes := request.EncodeToBytes()
	if err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout)); err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return nil, err
	}
	if err := writeFull(h.conn, bytes); err != nil { /* handle */
		log.Default().Printf("Error writing Modbus frame: %v", err)
		return nil, err
	}
	if err := h.conn.SetReadDeadline(time.Now().Add(h.readTimeout)); err != nil {
		log.Default().Printf("Error setting Modbus connection deadline: %v", err)
		return nil, nil
	}
	headerBytes := make([]byte, 7)
	if _, err := io.ReadFull(h.conn, headerBytes); err != nil {
		log.Default().Printf("Error reading header: %v", err)
		return nil, nil
	}
	length := int(binary.BigEndian.Uint16(headerBytes[4:6]))
	remaining := length - 1
	rest := make([]byte, remaining)
	if _, err := io.ReadFull(h.conn, rest); err != nil {
		log.Default().Printf("Error reading rest: %v", err)
		return nil, nil
	}
	mbapHeader := NewMbapHeaderFromBytes(headerBytes)
	response := NewModbusRegisterValueResponseFromBytes(request.RegisterDefinitions, request.JobId, request.TransactionId, request.RequestTime, mbapHeader, rest)
	log.Default().Printf("---------------------------------------")
	return response, nil
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
