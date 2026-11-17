package modbus

import (
	"bytes"
	"errors"
	"io"
	"net"
	"sync"
	"testing"
	"time"

	"pv-logger/retry"
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
	saved := busyWait
	busyWait = 10 * time.Millisecond
	t.Cleanup(func() { busyWait = saved })
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

// A device that starts listening only after the first attempts were refused,
// as an in-namespace server does for a pod kube-router has not admitted yet.
func TestTcpHandler_ConnectRetriesARefusedConnection(t *testing.T) {
	saved := refusedPolicy
	refusedPolicy = retry.Policy{Attempts: 10, Initial: 50 * time.Millisecond, Max: 50 * time.Millisecond}
	t.Cleanup(func() { refusedPolicy = saved })

	probe, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	addr := probe.Addr().String()
	_ = probe.Close()
	go func() {
		time.Sleep(200 * time.Millisecond)
		ln, err := net.Listen("tcp", addr)
		if err != nil {
			return
		}
		t.Cleanup(func() { _ = ln.Close() })
		conn, err := ln.Accept()
		if err == nil {
			t.Cleanup(func() { _ = conn.Close() })
		}
	}()

	h := NewTcpHandler(addr, time.Second, 0)
	if err := h.Connect(); err != nil {
		t.Fatalf("Connect: %v", err)
	}
	_ = h.Close()
}

// foreignFrame answers another request (transaction 5) with 62 register bytes,
// like the frame the full collector took for its 4-byte answer on 2026-11-16.
var foreignFrame = append([]byte{0x00, 0x05, 0x00, 0x00, 0x00, 0x41, 0x01, 0x03, 0x3e}, make([]byte, 62)...)

func answers(frames ...[]byte) func(net.Conn) {
	return func(c net.Conn) {
		for _, f := range frames {
			_, _ = c.Write(f)
		}
	}
}

func TestTcpHandler_AFrameForAnotherRequestIsSkipped(t *testing.T) {
	address, connections := serve(t, answers(foreignFrame, okResponse))
	h := newTestHandler(t, address)

	_, pdu, err := h.ExecuteRequest(request)
	if err != nil {
		t.Fatalf("ExecuteRequest: %v", err)
	}
	if want := okResponse[7:]; !bytes.Equal(pdu, want) {
		t.Errorf("pdu = % x, want % x", pdu, want)
	}
	if got := connections(); got != 1 {
		t.Errorf("connections = %d, want 1 (the right reply follows on the same connection)", got)
	}
}

func TestTcpHandler_AFrameForAnotherUnitIsSkipped(t *testing.T) {
	otherUnit := bytes.Clone(okResponse)
	otherUnit[6] = 0x02
	otherUnit[9] = 0x99 // a different value, so taking it would show
	address, _ := serve(t, answers(otherUnit, okResponse))
	h := newTestHandler(t, address)

	if _, pdu, err := h.ExecuteRequest(request); err != nil || !bytes.Equal(pdu, okResponse[7:]) {
		t.Fatalf("got % x, %v; want the reply of unit 1", pdu, err)
	}
}

// The 2026-11-16 sequence: busy, busy, then a foreign frame before the reply.
func TestTcpHandler_BusyTwiceThenAForeignFrameStillGetsTheReply(t *testing.T) {
	busy := answer(exceptionResponse)
	address, connections := serve(t, busy, busy, answers(foreignFrame, okResponse))
	h := newTestHandler(t, address)

	start := time.Now()
	_, pdu, err := h.ExecuteRequest(request)
	if err != nil {
		t.Fatalf("ExecuteRequest: %v", err)
	}
	if !bytes.Equal(pdu, okResponse[7:]) {
		t.Errorf("pdu = % x, want % x", pdu, okResponse[7:])
	}
	if got := connections(); got != 3 {
		t.Errorf("connections = %d, want 3", got)
	}
	// Two busy waits before the retries: busyWait, then twice that.
	if took := time.Since(start); took < 3*busyWait {
		t.Errorf("took %s, want at least %s of busy waits", took, 3*busyWait)
	}
}

func TestTcpHandler_OnlyForeignFramesIsAnError(t *testing.T) {
	address, _ := serve(t, answers(foreignFrame))
	h := newTestHandler(t, address)

	if _, _, err := h.ExecuteRequest(request); err == nil {
		t.Fatal("a reply to another request was accepted")
	}
}

func TestTcpHandler_AnExceptionForAnotherRequestIsSkipped(t *testing.T) {
	otherException := bytes.Clone(exceptionResponse)
	otherException[1] = 0x05
	address, _ := serve(t, answers(otherException, okResponse))
	h := newTestHandler(t, address)

	if _, pdu, err := h.ExecuteRequest(request); err != nil || !bytes.Equal(pdu, okResponse[7:]) {
		t.Fatalf("got % x, %v; want the reply after skipping the other request's exception", pdu, err)
	}
}
