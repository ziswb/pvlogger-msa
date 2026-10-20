package kpi

import (
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"pvlogger-kpi-exporter/internal/config"
)

// An unresponsive data accessor must fail the export instead of hanging the
// CronJob (security finding 40).
func TestFetchKpis_TimesOutOnStalledServer(t *testing.T) {
	stop := make(chan struct{})
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		select {
		case <-r.Context().Done():
		case <-stop:
		}
	}))
	defer srv.Close()
	defer close(stop) // runs first: lets the stalled handler return
	defer func(c *http.Client) { httpClient = c }(httpClient)
	httpClient = &http.Client{Timeout: 100 * time.Millisecond}

	u, _ := url.Parse(srv.URL)
	host, port, _ := net.SplitHostPort(u.Host)
	cfg := &config.Config{RestProtocol: u.Scheme, RestHost: host, RestPort: port, RestPath: "/kpi"}

	start := time.Now()
	if _, err := FetchKpis(cfg); err == nil {
		t.Fatal("expected a timeout error, got nil")
	}
	if elapsed := time.Since(start); elapsed > 2*time.Second {
		t.Errorf("expected to give up after ~100ms, took %s", elapsed)
	}
}

func TestFetchKpis_DefaultClientHasTimeout(t *testing.T) {
	if httpClient.Timeout != 30*time.Second {
		t.Errorf("expected a 30s timeout, got %s", httpClient.Timeout)
	}
}
