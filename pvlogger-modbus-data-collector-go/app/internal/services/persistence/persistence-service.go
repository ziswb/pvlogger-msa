package persistence

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/modbus/response"
)

func (s *PersistingService) StoreValues(valueResponse *response.RegisterValueResponse) error {
	log.Default().Println("Storing modbus values ...")
	storeModbusValuesPath := config.DataAccessorCfg().StoreModbusValuesPath
	return s.storeValues(valueResponse, storeModbusValuesPath)
}

func (s *PersistingService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) error {
	log.Default().Println("Storing kpi values ...")
	storeKpiValuesPath := config.DataAccessorCfg().StoreKpiValuesPath
	return s.storeValues(kpiValues, storeKpiValuesPath)
}

func (s *PersistingService) storeValues(values any, storagePath string) error {
	log.Default().Println("Storing values ...")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, config.DataAccessorCfg().BasePath, storagePath))
	if err != nil {
		log.Default().Fatal("Failed to parse URL", err)
	}
	log.Default().Printf("Store URL: %v", postURL)
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(values)
	if err != nil {
		log.Default().Fatal("Failed to encode object", err)
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		log.Default().Fatal("Failed to create request", err)
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		log.Default().Fatal("Failed to send request", err)
	}
	defer func(body io.ReadCloser) {
		log.Default().Printf("---------------------------------------")
		log.Default().Println("Closing response body ...")
		err := body.Close()
		if err != nil {
			log.Default().Printf("Failed to close response body: %v", err)
		}
		log.Default().Printf("---------------------------------------")
	}(serverResponse.Body)
	log.Default().Printf("Server response: %v", serverResponse)
	return nil
}
