package modbus

import (
	"net"
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
