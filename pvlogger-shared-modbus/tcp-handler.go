package modbus

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"pv-logger/logging"
	"pvlogger-shared-modbus/model"
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

// Close closes the connection. It is idempotent: a second call, or one without
// a connection, does nothing.
func (h *TcpHandler) Close() error {
	if h.conn == nil {
		return nil
	}
	logging.Debug("closing modbus connection")
	conn := h.conn
	h.conn = nil
	return conn.Close()
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

// maxAttempts is how often ExecuteRequest tries a request before giving up.
const maxAttempts = 3

// ExecuteRequest sends a request and returns the response's MBAP header and
// PDU (function code onwards). It succeeds only on a complete, non-exception
// response.
//
// Every retry runs on a fresh connection. After a timeout the device may still
// answer the abandoned request, and on the same connection that late frame (or
// a stray exception) would be read as the answer to the retry. That happened
// on 2026-11-14 13:02: attempt 1 timed out, attempt 2 read a two-byte exception
// PDU 5 ms later, and the collector stored an empty value set (HTTP 400).
func (h *TcpHandler) ExecuteRequest(request []byte) (*model.MbapHeader, []byte, error) {
	var lastErr error
	for attempt := 1; attempt <= maxAttempts; attempt++ {
		if attempt > 1 {
			if err := h.reconnect(); err != nil {
				logging.Error("error reconnecting to modbus device", "attempt", attempt, "error", err)
				lastErr = err
				continue
			}
		}
		logging.Info("executing request", "attempt", attempt)
		header, pdu, err := h.exchange(request)
		if err == nil {
			return header, pdu, nil
		}
		logging.Error("modbus request failed", "attempt", attempt, "error", err)
		lastErr = err
	}
	return nil, nil, fmt.Errorf("modbus request failed after %d attempts: %w", maxAttempts, lastErr)
}

// reconnect drops the current connection, whatever state it is in, and opens a
// new one.
func (h *TcpHandler) reconnect() error {
	if err := h.Close(); err != nil {
		logging.Warn("error closing modbus connection before retry", "error", err)
	}
	return h.Connect()
}

// ExceptionError is a Modbus exception response: the device understood the
// request and refused it (e.g. code 0x06, server device busy).
type ExceptionError struct {
	FunctionCode  byte
	ExceptionCode byte
}

func (e *ExceptionError) Error() string {
	return fmt.Sprintf("modbus exception response: function 0x%02x, exception code 0x%02x", e.FunctionCode, e.ExceptionCode)
}

// exchange performs one request/response round trip on the current connection.
func (h *TcpHandler) exchange(request []byte) (*model.MbapHeader, []byte, error) {
	if h.conn == nil {
		return nil, nil, errors.New("not connected")
	}
	if err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout)); err != nil {
		return nil, nil, fmt.Errorf("setting write deadline: %w", err)
	}
	if err := h.conn.SetReadDeadline(time.Now().Add(h.readTimeout)); err != nil {
		return nil, nil, fmt.Errorf("setting read deadline: %w", err)
	}
	if err := writeFull(h.conn, request); err != nil {
		return nil, nil, fmt.Errorf("writing request: %w", err)
	}
	headerBytes := make([]byte, 7)
	if _, err := io.ReadFull(h.conn, headerBytes); err != nil {
		return nil, nil, fmt.Errorf("reading header bytes: %w", err)
	}
	mbapHeader := model.NewMbapHeaderFromBytes(headerBytes)
	dataLength := int(binary.BigEndian.Uint16(headerBytes[4:6]))
	// The Length field is defined as the number of bytes following the first 6 bytes of the header
	// i.e., it includes the Logic Device ID (1 byte) + Function Code (1 byte) + Data
	// Since we have read 7 bytes (including the Logic Device ID), we need to subtract 1 byte from the data length
	remaining := dataLength - 1
	// Every response PDU has at least a function code and one more byte (the
	// byte count, or the exception code).
	if remaining < 2 {
		return nil, nil, fmt.Errorf("response too short: MBAP length %d", dataLength)
	}
	pduBytes := make([]byte, remaining)
	if _, err := io.ReadFull(h.conn, pduBytes); err != nil {
		return nil, nil, fmt.Errorf("reading response PDU: %w", err)
	}
	if pduBytes[0]&0x80 != 0 {
		return nil, nil, &ExceptionError{FunctionCode: pduBytes[0], ExceptionCode: pduBytes[1]}
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
