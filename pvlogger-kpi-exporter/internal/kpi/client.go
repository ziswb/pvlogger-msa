package kpi

import (
	"encoding/json"
	"io"
	"net"
	"net/http"
	"net/url"

	"pvlogger-go-shared/logging"
	"pvlogger-kpi-exporter/internal/config"
)

func FetchKpis(cfg *config.Config) *Values {
	u := &url.URL{
		Scheme: cfg.RestProtocol,
		Host:   net.JoinHostPort(cfg.RestHost, cfg.RestPort),
		Path:   cfg.RestPath,
	}
	targetURL := u.String()

	resp, err := http.Get(targetURL)
	if err != nil {
		logging.Fatal("failed to fetch KPIs", "error", err)
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
		logging.Fatal("failed to read response body", "error", readErr)
	}
	kpiValues := Values{}
	err = json.Unmarshal(body, &kpiValues)
	if err != nil {
		logging.Fatal("failed to unmarshal KPI values", "error", err)
	}
	return &kpiValues
}
