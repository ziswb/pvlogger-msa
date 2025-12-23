package services

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
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"time"
)

type PersistingService struct {
	baseURL    string
	httpClient *http.Client
}

func NewPersistingService(config *config.DataAccessorConfig) *PersistingService {
	return &PersistingService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.Protocol, config.Host, config.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *PersistingService) StoreModbusValues(response *modbus.ModbusRegisterValueResponse) error {
	log.Default().Println("Storing modbus values ...")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, config.DataAccessorCfg().BasePath, config.DataAccessorCfg().StoreModbusValuesPath))
	if err != nil {
		return fmt.Errorf("failed to parse store modbus values URL: %w", err)
	}
	log.Default().Println("Store modbus values post URL:", postURL)
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(response)
	if err != nil {
		log.Default().Println("Failed to encode modbus values object:", err)
		return err
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		log.Default().Println("Failed to create store modbus values request:", err)
		return err
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		log.Default().Println("Failed to send store modbus values request:", err)
		return err
	}
	defer func(Body io.ReadCloser) {
		log.Default().Printf("---------------------------------------")
		log.Default().Println("Closing response body ...")
		err := Body.Close()
		if err != nil {
			log.Default().Println("Failed to close response body:", err)
		}
		log.Default().Printf("---------------------------------------")
	}(serverResponse.Body)
	log.Default().Println("Server response:", serverResponse)
	return nil
}

func (s *PersistingService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) error {
	log.Default().Println("Storing kpi values ...")
	postURL, err := url.Parse(fmt.Sprintf("%s/%s/%s", s.baseURL, config.DataAccessorCfg().BasePath, config.DataAccessorCfg().StoreKpiValuesPath))
	if err != nil {
		log.Default().Println("Failed to parse store kpi URL:", err)
		return err
	}
	log.Default().Println("Store kpi values post URL:", postURL)
	var b bytes.Buffer
	err = json.NewEncoder(&b).Encode(kpiValues)
	if err != nil {
		log.Default().Println("Failed to encode kpi values object:", err)
		return err
	}
	request, err := http.NewRequest("POST", postURL.String(), &b)
	if err != nil {
		log.Default().Println("Failed to create store kpi values request:", err)
		return err
	}
	request.Header.Set("Content-Type", "application/json")
	serverResponse, err := s.httpClient.Do(request)
	if err != nil {
		log.Default().Println("Failed to send store kpi values request:", err)
		return err
	}
	defer func(Body io.ReadCloser) {
		log.Default().Printf("---------------------------------------")
		log.Default().Println("Closing response body ...")
		err := Body.Close()
		if err != nil {
			log.Default().Println("Failed to close response body:", err)
		}
		log.Default().Printf("---------------------------------------")
	}(serverResponse.Body)
	log.Default().Println("Server response:", serverResponse)
	return nil
}
