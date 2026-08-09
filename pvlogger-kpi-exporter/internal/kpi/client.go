package kpi

import (
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"

	"pv-logger/logging"
	"pvlogger-kpi-exporter/internal/config"
)

func FetchKpis(cfg *config.Config) (*Values, error) {
	u := &url.URL{
		Scheme: cfg.RestProtocol,
		Host:   net.JoinHostPort(cfg.RestHost, cfg.RestPort),
		Path:   cfg.RestPath,
	}
	targetURL := u.String()

	resp, err := http.Get(targetURL)
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
