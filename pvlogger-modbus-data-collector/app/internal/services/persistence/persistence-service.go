package persistence

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"pvlogger-go-shared/logging"
	"pvlogger-modbus-data-collector/internal/config"
	"pvlogger-modbus-data-collector/internal/kpis"
	"pvlogger-modbus-data-collector/internal/modbus/response"
)

func (s *PersistingService) StoreValues(valueResponse *response.RegisterValueResponse) {
	logging.Info("storing modbus values")
	storeModbusValuesPath := config.DataAccessorCfg().StoreModbusValuesPath
	s.storeValues(valueResponse, storeModbusValuesPath)
}

func (s *PersistingService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) {
	logging.Info("storing kpi values")
	storeKpiValuesPath := config.DataAccessorCfg().StoreKpiValuesPath
	s.storeValues(kpiValues, storeKpiValuesPath)
}

func (s *PersistingService) StoreDeviceIdentifierValues(resp *response.ReadDeviceIdentifiersResponse) {
	logging.Info("storing device identifier values")
	storeDevIdValuesPath := config.DataAccessorCfg().StoreDeviceIdValuesPath
	s.storeValues(resp, storeDevIdValuesPath)
}

func (s *PersistingService) storeValues(values any, storagePath string) {
	logging.Info("storing values")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, config.DataAccessorCfg().BasePath, storagePath))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	logging.Debug("store request", "url", postURL.String())
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(values)
	if err != nil {
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
	defer func(body io.ReadCloser) {
		logging.Debug("closing response body")
		err := body.Close()
		if err != nil {
			logging.Error("failed to close response body", "error", err)
		}
	}(serverResponse.Body)
	logging.Info("values stored", "status", serverResponse.Status)
}
