package httpclient

import (
	"io"
	"net"
	"net/http"
	"strings"
	"testing"
	"time"

	"pv-logger/retry"
)

// freeAddr returns a local address nothing listens on (yet).
func freeAddr(t *testing.T) string {
	t.Helper()
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	addr := ln.Addr().String()
	_ = ln.Close()
	return addr
}

func fastClient() *http.Client {
	return &http.Client{
		Timeout:   5 * time.Second,
		Transport: &retryRefused{next: http.DefaultTransport, policy: retry.Policy{Attempts: 10, Initial: 50 * time.Millisecond, Max: 50 * time.Millisecond}},
	}
}

// The server starts listening only after the first attempts were refused, as
// for a new pod whose IP the policy controller has not admitted yet.
func TestARefusedPostIsRetriedWithItsBody(t *testing.T) {
	addr := freeAddr(t)
	got := make(chan string, 1)
	go func() {
		time.Sleep(200 * time.Millisecond)
		ln, err := net.Listen("tcp", addr)
		if err != nil {
			got <- "listen: " + err.Error()
			return
		}
		srv := &http.Server{Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			b, _ := io.ReadAll(r.Body)
			got <- string(b)
		})}
		t.Cleanup(func() { _ = srv.Close() })
		_ = srv.Serve(ln)
	}()

	resp, err := fastClient().Post("http://"+addr+"/store", "application/json", strings.NewReader(`{"a":1}`))
	if err != nil {
		t.Fatal(err)
	}
	_ = resp.Body.Close()
	if body := <-got; body != `{"a":1}` {
		t.Errorf("server got body %q, want the original", body)
	}
}

func TestRefusalsStopAfterTheLastAttempt(t *testing.T) {
	addr := freeAddr(t)
	start := time.Now()
	_, err := fastClient().Get("http://" + addr + "/")
	if err == nil {
		t.Fatal("no error from a server that never listens")
	}
	// 9 waits, each jittered to 25–50 ms.
	if took := time.Since(start); took < 9*25*time.Millisecond || took > 3*time.Second {
		t.Errorf("gave up after %s, want 9 waits of 25–50ms", took)
	}
}
