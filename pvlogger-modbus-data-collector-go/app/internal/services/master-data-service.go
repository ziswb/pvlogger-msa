package services

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"strings"
	"time"
)

const (
	paramDevTypes       = "deviceTypes"
	paramCategories     = "categories"
	paramValueSeparator = ";"
)

type MasterDataService struct {
	baseURL    string
	httpClient *http.Client
}

func NewMasterDataService(config *config.MasterDataConfig) *MasterDataService {
	return &MasterDataService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.Protocol, config.Host, config.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *MasterDataService) FetchRegisterDefinitions(
	deviceTypes []string,
	categories []string,
) (*[]modbus.ModbusRegisterDefinition, error) {
	log.Default().Println("Fetching master data ...")
	devTypesParam := strings.Join(deviceTypes, paramValueSeparator)
	categoriesParam := strings.Join(categories, paramValueSeparator)

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

	log.Default().Println("Response status:" + resp.Status)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var definitions []modbus.ModbusRegisterDefinition
	if err := json.Unmarshal(body, &definitions); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	log.Default().Println("Fetched master data successfully.")
	return &definitions, nil
}
