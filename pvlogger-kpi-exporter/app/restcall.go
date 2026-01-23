package main

import (
	"encoding/json"
	"io"
	"net/http"

	"pvlogger-go-shared/logging"
)

func fetchKpis() *KpiValues {
	host := getEnv("REST_HOST", "localhost")
	port := getEnv("REST_PORT", "8083")
	path := getEnv("REST_PATH", "/v1/read-latest-kpi")
	protocol := getEnv("REST_PROTOCOL", "http")
	url := protocol + "://" + host + ":" + port + path

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
	kpiValues := KpiValues{}
	err = json.Unmarshal(body, &kpiValues)
	if err != nil {
		logging.Fatal("failed to unmarshal KPI values", "error", err)
	}
	return &kpiValues
}
