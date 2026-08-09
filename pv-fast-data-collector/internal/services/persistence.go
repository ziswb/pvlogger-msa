package services

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"pv-fast-data-collector/internal/config"
	"pvlogger-shared-modbus/kpis"
	"pv-logger/logging"
	"time"
)

type RawKpiRegisterValues struct {
	JobId               string  `json:"jobId"`
	InverterActivePower float64 `json:"inverterActivePower"`
	MeterActivePower    float64 `json:"meterActivePower"`
	InverterDailyYield  float64 `json:"inverterDailyYield"`
	MeterExportedEnergy float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy float64 `json:"meterImportedEnergy"`
}

type MeterEnergyTodayResponse struct {
	ExportedEnergyToday float64 `json:"exportedEnergyToday"`
	ImportedEnergyToday float64 `json:"importedEnergyToday"`
}

type PersistenceService struct {
	baseURL    string
	httpClient *http.Client
}

func NewPersistenceService() *PersistenceService {
	cfg := config.DataAccessorCfg()
	return &PersistenceService{
		baseURL: fmt.Sprintf("%s://%s:%d", cfg.Protocol, cfg.Host, cfg.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *PersistenceService) StoreRawValues(values *RawKpiRegisterValues) error {
	logging.Info("storing raw KPI register values")
	cfg := config.DataAccessorCfg()
	return s.postJSON(values, cfg.StoreRawKpiRegisterValues)
}

func (s *PersistenceService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) error {
	logging.Info("storing KPI values")
	cfg := config.DataAccessorCfg()
	return s.postJSON(kpiValues, cfg.StoreKpiValuesPath)
}

func (s *PersistenceService) ReadMeterEnergyToday() (*MeterEnergyTodayResponse, error) {
	logging.Info("reading meter energy today")
	cfg := config.DataAccessorCfg()
	getURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, cfg.BasePath, cfg.ReadMeterEnergyTodayPath))
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL: %w", err)
	}
	if logging.DebugEnabled() {
		logging.Debug("GET request", "url", getURL.String())
	}
	resp, err := s.httpClient.Get(getURL.String())
	if err != nil {
		return nil, fmt.Errorf("failed to send GET request: %w", err)
	}
	defer closeBody(resp)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("error reading meter energy today: unexpected status %s", resp.Status)
	}
	var result MeterEnergyTodayResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode meter energy today response: %w", err)
	}
	logging.Info("meter energy today", "exported", result.ExportedEnergyToday, "imported", result.ImportedEnergyToday)
	return &result, nil
}

func (s *PersistenceService) postJSON(values any, storagePath string) error {
	cfg := config.DataAccessorCfg()
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, cfg.BasePath, storagePath))
	if err != nil {
		return fmt.Errorf("failed to parse URL: %w", err)
	}
	logging.Debug("POST request", "url", postURL.String())
	var b bytes.Buffer
	if err := json.NewEncoder(&b).Encode(values); err != nil {
		return fmt.Errorf("failed to encode object: %w", err)
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		return fmt.Errorf("failed to send request: %w", err)
	}
	defer closeBody(serverResponse)
	logging.Info("values stored", "status", serverResponse.Status)
	return nil
}

func closeBody(resp *http.Response) {
	if resp != nil && resp.Body != nil {
		logging.Debug("closing response body")
		if err := resp.Body.Close(); err != nil {
			logging.Error("failed to close response body", "error", err)
		}
	}
}
