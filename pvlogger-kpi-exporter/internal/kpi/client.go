package kpi

import (
	"encoding/json"
	"io"
	"net/http"

	"pvlogger-go-shared/logging"
	"pvlogger-kpi-exporter/internal/config"
)

func FetchKpis(cfg *config.Config) *Values {
	url := cfg.Protocol + "://" + cfg.RestHost + ":" + cfg.RestPort + cfg.RestPath

	resp, err := http.Get(url)
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
	logging.Debug("received message", "body", string(body))
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
