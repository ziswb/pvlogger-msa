package modbus

import (
	"encoding/binary"
	"io"
	"net"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
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
	logging.Debug("closing modbus connection")
	if h.conn == nil {
		return nil
	}
	return h.conn.Close()
}

func (h *TcpHandler) Connect() error {
	logging.Info("connecting to modbus device", "address", h.address)
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
	if err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout)); err != nil {
		logging.Error("error setting modbus connection write deadline", "error", err)
		return err
	}
	if err := h.conn.SetReadDeadline(time.Now().Add(h.readTimeout)); err != nil {
		logging.Error("error setting modbus connection read deadline", "error", err)
		return err
	}
	logging.Debug("connected to modbus device")
	return nil
}

func (h *TcpHandler) ExecuteRequest(bytes []byte) (*model.MbapHeader, []byte, error) {
	logging.Info("executing request")
	if err := writeFull(h.conn, bytes); err != nil {
		logging.Error("error writing bytes", "error", err)
		return nil, nil, err
	}
	headerBytes := make([]byte, 7)
	if _, err := io.ReadFull(h.conn, headerBytes); err != nil {
		logging.Error("error reading header bytes", "error", err)
		return nil, nil, err
	}
	mbapHeader := model.NewMbapHeaderFromBytes(headerBytes)
	dataLength := int(binary.BigEndian.Uint16(headerBytes[4:6]))
	// The Length field is defined as the number of bytes following the first 6 bytes of the header
	// i.e., it includes the Logic Device ID (1 byte) + Function Code (1 byte) + Data
	// Since we have read 7 bytes (including the Logic Device ID), we need to subtract 1 byte from the data length
	remaining := dataLength - 1
	pduBytes := make([]byte, remaining)
	if _, err := io.ReadFull(h.conn, pduBytes); err != nil {
		logging.Error("error reading rest", "error", err)
		return nil, nil, err
	}
	return &mbapHeader, pduBytes, nil
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
