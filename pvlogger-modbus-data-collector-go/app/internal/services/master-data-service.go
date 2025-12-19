package services

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/app/internal/config"
	"pvlogger-modbus-data-collector-go/app/internal/model"
	"strings"
	"time"
)

const (
	paramDevTypes       = "deviceTypes"
	paramCategories     = "categories"
	paramValueSeparator = ";"
)

func joinStrings(categories []string) string {
	strs := make([]string, len(categories))
	for i, c := range categories {
		strs[i] = c
	}
	return strings.Join(strs, paramValueSeparator)
}

type MasterDataConfig struct {
	Protocol string
	Host     string
	Port     int
}

type MasterDataService struct {
	baseURL    string
	httpClient *http.Client
}

func NewMasterDataService(config *MasterDataConfig) *MasterDataService {
	return &MasterDataService{
		baseURL: fmt.Sprintf("http://%s:%d", config.Host, config.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *MasterDataService) FetchRegisterDefinitions(
	deviceTypes []string,
	categories []string,
) (*[]model.ModbusRegisterDefinition, error) {
	log.Default().Println("Fetching master data...")
	devTypesParam := joinStrings(deviceTypes)
	categoriesParam := joinStrings(categories)

	reqURL, err := url.Parse(s.baseURL + config.MdConfig().MasterDataPath)
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL: %w", err)
	}

	q := reqURL.Query()
	q.Set(paramDevTypes, devTypesParam)
	q.Set(paramCategories, categoriesParam)
	reqURL.RawQuery = q.Encode()

	log.Default().Println(reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	if err != nil {
		return nil, fmt.Errorf("failed to fetch master data: %w", err)
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
		}
	}(resp.Body)

	log.Default().Println("Response status: " + resp.Status)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}

	log.Default().Println("Response body:")
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var definitions []model.ModbusRegisterDefinition
	if err := json.Unmarshal(body, &definitions); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	log.Default().Println("Fetched master data successfully.")
	return &definitions, nil
}
