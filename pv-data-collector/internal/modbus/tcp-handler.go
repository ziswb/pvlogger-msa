package modbus

import (
	"encoding/binary"
	"io"
	"net"
	"pv-data-collector/internal/modbus/model"
	"pvlogger-go-shared/logging"
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

func NewTcpHandler(url string, timeout time.Duration, connectionSleep time.Duration) *TcpHandler {
	return &TcpHandler{
		address:      url,
		timeout:      timeout,
		sleep:        connectionSleep,
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
	logging.Debug("connected to modbus device")
	return nil
}

func (h *TcpHandler) ExecuteRequest(bytes []byte) (*model.MbapHeader, []byte, error) {
	const maxAttempts = 3
	var lastErr error
	for attempt := 1; attempt <= maxAttempts; attempt++ {
		logging.Info("executing request", "attempt", attempt)
		if err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout)); err != nil {
			logging.Error("error resetting write deadline", "attempt", attempt, "error", err)
			lastErr = err
			continue
		}
		if err := h.conn.SetReadDeadline(time.Now().Add(h.readTimeout)); err != nil {
			logging.Error("error resetting read deadline", "attempt", attempt, "error", err)
			lastErr = err
			continue
		}
		if err := writeFull(h.conn, bytes); err != nil {
			logging.Error("error writing bytes", "attempt", attempt, "error", err)
			lastErr = err
			continue
		}
		headerBytes := make([]byte, 7)
		if _, err := io.ReadFull(h.conn, headerBytes); err != nil {
			logging.Error("error reading header bytes", "attempt", attempt, "error", err)
			lastErr = err
			continue
		}
		mbapHeader := model.NewMbapHeaderFromBytes(headerBytes)
		dataLength := int(binary.BigEndian.Uint16(headerBytes[4:6]))
		remaining := dataLength - 1
		pduBytes := make([]byte, remaining)
		if _, err := io.ReadFull(h.conn, pduBytes); err != nil {
			logging.Error("error reading rest", "attempt", attempt, "error", err)
			lastErr = err
			continue
		}
		return &mbapHeader, pduBytes, nil
	}
	return nil, nil, lastErr
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
