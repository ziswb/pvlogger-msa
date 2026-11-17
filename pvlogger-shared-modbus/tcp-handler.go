package modbus

import (
	"context"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"pv-logger/logging"
	"pv-logger/retry"
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

// Connect connects to the device, retrying a refused connect (refusedPolicy).
// No context: the collectors run one short job and exit (backlog 62).
func (h *TcpHandler) Connect() error {
	ctx := context.Background()
	logging.Info("connecting to modbus device", "address", h.address)
	d := &net.Dialer{Timeout: h.timeout, KeepAlive: 30 * time.Second}
	conn, err := retry.OnRefused(ctx, refusedPolicy, "modbus connect", func(ctx context.Context) (net.Conn, error) {
		return d.DialContext(ctx, "tcp", h.address)
	}, "address", h.address)
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

// refusedPolicy bounds how long a refused connect is retried. In the cluster a
// new pod reaches an in-namespace server (the dev mock) only once kube-router
// has admitted its IP, a moment after the start (backlog 58); a busy device may
// refuse briefly too. Shorter than retry.Refused (15 s) on purpose: every
// retry of ExecuteRequest reconnects, so a device that keeps refusing would
// cost a request two such budgets and push a run past its minute (backlog 62).
// 4 attempts, at most 0.5+1+2 = 3.5 s of waiting. A variable for the tests.
var refusedPolicy = retry.Policy{Attempts: 4, Initial: 500 * time.Millisecond, Max: 2 * time.Second}

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
//
// A busy exception (0x06) waits busyWait before the second attempt and twice
// that before the third; frames that answer another request are skipped
// (exchange, backlog 61).
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
		// A busy device needs time, not an immediate second request (backlog 61).
		var exc *ExceptionError
		if attempt < maxAttempts && errors.As(err, &exc) && exc.ExceptionCode == exceptionServerBusy {
			wait := busyWait << (attempt - 1)
			logging.Warn("modbus device busy, waiting before the retry", "wait", wait.String())
			time.Sleep(wait)
		}
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

// exceptionServerBusy is the Modbus exception "server device busy".
const exceptionServerBusy = 0x06

// busyWait is the wait before the second attempt after a busy exception; it
// doubles for the third. A variable for the tests.
var busyWait = time.Second

// ExceptionError is a Modbus exception response: the device understood the
// request and refused it (e.g. code 0x06, server device busy).
type ExceptionError struct {
	FunctionCode  byte
	ExceptionCode byte
}

func (e *ExceptionError) Error() string {
	return fmt.Sprintf("modbus exception response: function 0x%02x, exception code 0x%02x", e.FunctionCode, e.ExceptionCode)
}

// maxForeignFrames bounds how many frames that answer another request one
// exchange skips before giving up (the read deadline bounds it in time too).
const maxForeignFrames = 16

// exchange performs one request/response round trip on the current connection.
//
// The reply must carry the request's transaction ID, unit ID and function code
// (or its exception form). A frame that does not answers another request: a
// late reply to one the device queued while it was busy, or a stray frame from
// before a reconnect. It is logged and skipped, and reading goes on until the
// read deadline. On 2026-11-16 such a 62-byte frame was taken as the answer to
// a 4-byte read (backlog 61).
func (h *TcpHandler) exchange(request []byte) (*model.MbapHeader, []byte, error) {
	if h.conn == nil {
		return nil, nil, errors.New("not connected")
	}
	if len(request) < 8 {
		return nil, nil, fmt.Errorf("request too short: %d bytes", len(request))
	}
	wantTx := binary.BigEndian.Uint16(request[0:2])
	wantUnit, wantFunction := request[6], request[7]
	if err := h.conn.SetWriteDeadline(time.Now().Add(h.writeTimeout)); err != nil {
		return nil, nil, fmt.Errorf("setting write deadline: %w", err)
	}
	if err := h.conn.SetReadDeadline(time.Now().Add(h.readTimeout)); err != nil {
		return nil, nil, fmt.Errorf("setting read deadline: %w", err)
	}
	if err := writeFull(h.conn, request); err != nil {
		return nil, nil, fmt.Errorf("writing request: %w", err)
	}
	for skipped := 0; ; skipped++ {
		headerBytes, pduBytes, err := h.readFrame()
		if err != nil {
			return nil, nil, err
		}
		gotTx, gotUnit, gotFunction := binary.BigEndian.Uint16(headerBytes[0:2]), headerBytes[6], pduBytes[0]
		if gotTx != wantTx || gotUnit != wantUnit || gotFunction&0x7f != wantFunction {
			logging.Warn("skipping a modbus frame that answers another request",
				"want_transaction", wantTx, "got_transaction", gotTx,
				"want_unit", wantUnit, "got_unit", gotUnit,
				"want_function", wantFunction, "got_function", gotFunction, "pdu_bytes", len(pduBytes))
			if skipped+1 >= maxForeignFrames {
				return nil, nil, fmt.Errorf("no reply to transaction %d among %d frames", wantTx, maxForeignFrames)
			}
			continue
		}
		if gotFunction&0x80 != 0 {
			return nil, nil, &ExceptionError{FunctionCode: gotFunction, ExceptionCode: pduBytes[1]}
		}
		mbapHeader := model.NewMbapHeaderFromBytes(headerBytes)
		return &mbapHeader, pduBytes, nil
	}
}

// readFrame reads one MBAP frame: the 7-byte header (with the unit ID) and the
// PDU from the function code onwards.
func (h *TcpHandler) readFrame() ([]byte, []byte, error) {
	headerBytes := make([]byte, 7)
	if _, err := io.ReadFull(h.conn, headerBytes); err != nil {
		return nil, nil, fmt.Errorf("reading header bytes: %w", err)
	}
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
	return headerBytes, pduBytes, nil
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
