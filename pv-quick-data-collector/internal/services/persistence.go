package services

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"pv-quick-data-collector/internal/config"
	"pv-quick-data-collector/internal/kpis"
	"pvlogger-go-shared/logging"
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

func (s *PersistenceService) StoreRawValues(values *RawKpiRegisterValues) {
	logging.Info("storing raw KPI register values")
	cfg := config.DataAccessorCfg()
	s.postJSON(values, cfg.StoreRawKpiRegisterValues)
}

func (s *PersistenceService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) {
	logging.Info("storing KPI values")
	cfg := config.DataAccessorCfg()
	s.postJSON(kpiValues, cfg.StoreKpiValuesPath)
}

func (s *PersistenceService) ReadMeterEnergyToday() *MeterEnergyTodayResponse {
	logging.Info("reading meter energy today")
	cfg := config.DataAccessorCfg()
	getURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, cfg.BasePath, cfg.ReadMeterEnergyTodayPath))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	if logging.DebugEnabled() {
		logging.Debug("GET request", "url", getURL.String())
	}
	resp, err := s.httpClient.Get(getURL.String())
	if err != nil {
		logging.Fatal("failed to send GET request", "error", err)
	}
	defer closeBody(resp)
	if resp.StatusCode != http.StatusOK {
		logging.Fatal("error reading meter energy today", "status", resp.Status)
	}
	var result MeterEnergyTodayResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		logging.Fatal("failed to decode meter energy today response", "error", err)
	}
	logging.Info("meter energy today", "exported", result.ExportedEnergyToday, "imported", result.ImportedEnergyToday)
	return &result
}

func (s *PersistenceService) postJSON(values any, storagePath string) {
	cfg := config.DataAccessorCfg()
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, cfg.BasePath, storagePath))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	logging.Debug("POST request", "url", postURL.String())
	var b bytes.Buffer
	if err := json.NewEncoder(&b).Encode(values); err != nil {
		logging.Fatal("failed to encode object", "error", err)
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		logging.Fatal("failed to create request", "error", err)
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		logging.Fatal("failed to send request", "error", err)
	}
	defer closeBody(serverResponse)
	logging.Info("values stored", "status", serverResponse.Status)
}

func closeBody(resp *http.Response) {
	if resp != nil && resp.Body != nil {
		logging.Debug("closing response body")
		if err := resp.Body.Close(); err != nil {
			logging.Error("failed to close response body", "error", err)
		}
	}
}


