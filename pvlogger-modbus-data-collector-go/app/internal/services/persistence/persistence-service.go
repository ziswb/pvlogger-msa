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
		log.Default().Println("Failed to parse URL:", err)
		return err
	}
	log.Default().Println("Store URL:", postURL)
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(values)
	if err != nil {
		log.Default().Println("Failed to encode object:", err)
		return err
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		log.Default().Println("Failed to create request:", err)
		return err
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		log.Default().Println("Failed to send request:", err)
		return err
	}
	defer func(body io.ReadCloser) {
		log.Default().Printf("---------------------------------------")
		log.Default().Println("Closing response body ...")
		err := body.Close()
		if err != nil {
			log.Default().Println("Failed to close response body:", err)
		}
		log.Default().Printf("---------------------------------------")
	}(serverResponse.Body)
	log.Default().Println("Server response:", serverResponse)
	return nil
}
