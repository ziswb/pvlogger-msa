package kpi

import (
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"time"

	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/config"
)

// httpClient bounds the call to the data accessor, reading the body included.
// http.Get uses http.DefaultClient, which has no timeout, so an unresponsive
// data accessor would hang the CronJob (security finding 40). 30 s, like the other
// Go HTTP clients in the repo. A variable only so tests can shorten it.
var httpClient = &http.Client{Timeout: 30 * time.Second}

func FetchKpis(cfg *config.Config) (*Values, error) {
	u := &url.URL{
		Scheme: cfg.RestProtocol,
		Host:   net.JoinHostPort(cfg.RestHost, cfg.RestPort),
		Path:   cfg.RestPath,
	}
	targetURL := u.String()

	resp, err := httpClient.Get(targetURL)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch KPIs: %w", err)
	}
	defer func(body io.ReadCloser) {
		err := body.Close()
		if err != nil {
			logging.Error("failed to close response body", "error", err)
		}
	}(resp.Body)
	logging.Info("received response", "status", resp.Status)
	body, readErr := io.ReadAll(resp.Body)
	if logging.DebugEnabled() {
		logging.Debug("received message", "body", string(body))
	}
	if readErr != nil {
		return nil, fmt.Errorf("failed to read response body: %w", readErr)
	}
	kpiValues := Values{}
	err = json.Unmarshal(body, &kpiValues)
	if err != nil {
		return nil, fmt.Errorf("failed to unmarshal KPI values: %w", err)
	}
	return &kpiValues, nil
}
