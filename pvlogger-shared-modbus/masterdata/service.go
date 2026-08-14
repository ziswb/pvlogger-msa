package masterdata

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"pv-logger/logging"
	"pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/model"
	"strings"
	"time"
)

const (
	paramDevTypes       = "deviceTypes"
	paramCategories     = "categories"
	paramValueSeparator = ";"
)

type MasterDataService struct {
	baseURL        string
	masterDataPath string
	httpClient     *http.Client
}

func NewMasterDataService() *MasterDataService {
	cfg := config.MdConfig()
	return &MasterDataService{
		baseURL:        fmt.Sprintf("%s://%s:%d", cfg.Protocol, cfg.Host, cfg.Port),
		masterDataPath: config.MasterDataPath,
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func (s *MasterDataService) FetchRegisterDefinitions(
	deviceTypes []model.DeviceType,
	categories []model.Category,
) (*[]model.RegisterDefinition, error) {
	logging.Info("fetching master data")
	devTypesParam := strings.Join(arrayOfDeviceTypesToArrayOfStrings(deviceTypes), paramValueSeparator)
	categoriesParam := strings.Join(arrayOfCategoriesToArrayOfStrings(categories), paramValueSeparator)

	reqURL, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, s.masterDataPath))
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL: %w", err)
	}

	q := reqURL.Query()
	q.Set(paramDevTypes, devTypesParam)
	q.Set(paramCategories, categoriesParam)
	reqURL.RawQuery = q.Encode()

	logging.Debug("fetching master data", "url", reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	if err != nil {
		return nil, fmt.Errorf("failed to fetch master data: %w", err)
	}
	defer s.close(resp)

	logging.Debug("response received", "status", resp.Status)
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code %d fetching master data", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}
	var definitions []model.RegisterDefinition
	if err := json.Unmarshal(body, &definitions); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	logging.Info("fetched master data successfully")
	return &definitions, nil
}

func (s *MasterDataService) close(response *http.Response) {
	if response != nil && response.Body != nil {
		err := response.Body.Close()
		if err != nil {
			logging.Error("error closing response body", "error", err)
		}
	}
}

func arrayOfCategoriesToArrayOfStrings(categories []model.Category) []string {
	var texts []string
	for _, text := range categories {
		texts = append(texts, text.String())
	}
	return texts
}

func arrayOfDeviceTypesToArrayOfStrings(deviceTypes []model.DeviceType) []string {
	var texts []string
	for _, text := range deviceTypes {
		texts = append(texts, text.String())
	}
	return texts
}
