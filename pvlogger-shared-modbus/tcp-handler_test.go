package modbus

import (
	"bytes"
	"errors"
	"io"
	"net"
	"sync"
	"testing"
	"time"
)

// listen starts a TCP server that accepts connections and holds them until
// the test ends.
func listen(t *testing.T) string {
	t.Helper()
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}
	t.Cleanup(func() { _ = ln.Close() })
	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				return
			}
			t.Cleanup(func() { _ = conn.Close() })
		}
	}()
	return ln.Addr().String()
}

func TestTcpHandler_CloseTwiceIsNoError(t *testing.T) {
	h := NewTcpHandler(listen(t), time.Second, 0)
	if err := h.Connect(); err != nil {
		t.Fatalf("connect: %v", err)
	}
	if err := h.Close(); err != nil {
		t.Fatalf("first close: %v", err)
	}
	// A second close used to return net.ErrClosed, which the device collectors
	// logged as "modbus connection already closed".
	if err := h.Close(); err != nil {
		t.Errorf("second close: %v, want nil", err)
	}
}

func TestTcpHandler_CloseWithoutConnectIsNoError(t *testing.T) {
	h := NewTcpHandler("127.0.0.1:1", time.Second, 0)
	if err := h.Close(); err != nil {
		t.Errorf("close: %v, want nil", err)
	}
}

func TestTcpHandler_ReconnectAfterClose(t *testing.T) {
	h := NewTcpHandler(listen(t), time.Second, 0)
	for i := 1; i <= 2; i++ {
		if err := h.Connect(); err != nil {
			t.Fatalf("connect %d: %v", i, err)
		}
		if err := h.Close(); err != nil {
			t.Fatalf("close %d: %v", i, err)
		}
	}
}

// request is a read holding registers request for one register (transaction 7).
var request = []byte{0x00, 0x07, 0x00, 0x00, 0x00, 0x06, 0x01, 0x03, 0x7d, 0x40, 0x00, 0x01}

// okResponse answers request with the value 0x1234.
var okResponse = []byte{0x00, 0x07, 0x00, 0x00, 0x00, 0x05, 0x01, 0x03, 0x02, 0x12, 0x34}

// exceptionResponse refuses request with exception 0x06 (server device busy).
var exceptionResponse = []byte{0x00, 0x07, 0x00, 0x00, 0x00, 0x03, 0x01, 0x83, 0x06}

// serve starts a server whose n-th accepted connection (from 0) reads one
// request and is then handled by behaviours[n]; later connections use the last
// behaviour. It returns the address and a function counting the connections.
func serve(t *testing.T, behaviours ...func(net.Conn)) (string, func() int) {
	t.Helper()
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}
	t.Cleanup(func() { _ = ln.Close() })
	var mu sync.Mutex
	accepted := 0
	go func() {
		for {
			conn, err := ln.Accept()
			if err != nil {
				return
			}
			t.Cleanup(func() { _ = conn.Close() })
			mu.Lock()
			b := behaviours[min(accepted, len(behaviours)-1)]
			accepted++
			mu.Unlock()
			go func() {
				buf := make([]byte, len(request))
				if _, err := io.ReadFull(conn, buf); err != nil {
					return
				}
				b(conn)
			}()
		}
	}()
	return ln.Addr().String(), func() int {
		mu.Lock()
		defer mu.Unlock()
		return accepted
	}
}

func answer(frame []byte) func(net.Conn) {
	return func(c net.Conn) { _, _ = c.Write(frame) }
}

func newTestHandler(t *testing.T, address string) *TcpHandler {
	t.Helper()
	h := NewTcpHandler(address, time.Second, 0)
	h.readTimeout = 100 * time.Millisecond
	if err := h.Connect(); err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(func() { _ = h.Close() })
	return h
}

func TestTcpHandler_RetryAfterTimeoutUsesNewConnection(t *testing.T) {
	// The 2026-11-14 13:02 failure: the device misses the read deadline and
	// answers the abandoned request late. A retry on the same connection
	// would read that late frame; it must run on a new connection.
	late := func(c net.Conn) {
		time.Sleep(150 * time.Millisecond)
		_, _ = c.Write(exceptionResponse)
	}
	address, connections := serve(t, late, answer(okResponse))
	h := newTestHandler(t, address)

	_, pdu, err := h.ExecuteRequest(request)
	if err != nil {
		t.Fatalf("ExecuteRequest: %v", err)
	}
	if want := okResponse[7:]; !bytes.Equal(pdu, want) {
		t.Errorf("pdu = % x, want % x", pdu, want)
	}
	if got := connections(); got != 2 {
		t.Errorf("connections = %d, want 2 (retry on a fresh connection)", got)
	}
}

func TestTcpHandler_ExceptionResponseIsRetriedThenAnError(t *testing.T) {
	address, connections := serve(t, answer(exceptionResponse))
	h := newTestHandler(t, address)

	_, _, err := h.ExecuteRequest(request)
	var exc *ExceptionError
	if !errors.As(err, &exc) {
		t.Fatalf("ExecuteRequest error = %v, want an ExceptionError", err)
	}
	if exc.FunctionCode != 0x83 || exc.ExceptionCode != 0x06 {
		t.Errorf("exception = %+v, want function 0x83, code 0x06", exc)
	}
	if got := connections(); got != maxAttempts {
		t.Errorf("connections = %d, want %d (one per attempt)", got, maxAttempts)
	}
}

func TestTcpHandler_ExceptionThenSuccess(t *testing.T) {
	address, _ := serve(t, answer(exceptionResponse), answer(okResponse))
	h := newTestHandler(t, address)

	if _, _, err := h.ExecuteRequest(request); err != nil {
		t.Fatalf("ExecuteRequest: %v", err)
	}
}
