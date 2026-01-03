package masterdata

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strings"
)

func (s *MasterDataService) FetchRegisterDefinitions(
	deviceTypes []model.DeviceType,
	categories []model.Category,
) *[]model.RegisterDefinition {
	logging.Info("fetching master data")
	devTypesParam := strings.Join(arrayOfDeviceTypesToArrayOfStrings(deviceTypes), paramValueSeparator)
	categoriesParam := strings.Join(arrayOfCategoriesToArrayOfStrings(categories), paramValueSeparator)

	reqURL, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, config.MdConfig().MasterDataPath))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}

	q := reqURL.Query()
	q.Set(paramDevTypes, devTypesParam)
	q.Set(paramCategories, categoriesParam)
	reqURL.RawQuery = q.Encode()

	logging.Debug("fetching master data", "url", reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	defer s.close(resp)
	if err != nil {
		logging.Fatal("failed to fetch master data", "error", err)
	}

	logging.Debug("response received", "status", resp.Status)
	if resp.StatusCode != http.StatusOK {
		logging.Fatal("unexpected status code", "status_code", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		logging.Fatal("failed to read response body", "error", err)
	}
	var definitions []model.RegisterDefinition
	if err := json.Unmarshal(body, &definitions); err != nil {
		logging.Fatal("failed to unmarshal response", "error", err)
	}

	logging.Info("fetched master data successfully")
	return &definitions
}

func (s *MasterDataService) MasterDataForSmartMeterActivePower() (*model.RegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterActivePowerRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForInverterActivePower() (*model.RegisterDefinition, error) {
	return s.fetchMasterDataFor(inverterActivePowerRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForInverterEnergyYield() (*model.RegisterDefinition, error) {
	return s.fetchMasterDataFor(inverterDailyEnergyYieldRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForMeterExportedEnergy() (*model.RegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterExportedEnergyRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForMeterImportedEnergy() (*model.RegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterImportedEnergyRegisterAddress.Value)
}

func (s *MasterDataService) fetchMasterDataFor(registerAddress int) (*model.RegisterDefinition, error) {
	baseURL := s.baseURL
	masterDataPath := config.MdConfig().MasterDataPath
	reqURL, err := url.Parse(fmt.Sprintf("%s/%s/%d", baseURL, masterDataPath, registerAddress))
	if err != nil {
		logging.Fatal("failed to parse URL", "error", err)
	}
	logging.Debug("retrieving master data", "url", reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	defer s.close(resp)
	if err != nil {
		logging.Fatal("error retrieving master data", "error", err)
	}
	logging.Debug("response received", "status", resp.Status)
	if resp.StatusCode != http.StatusOK {
		logging.Fatal("unexpected status code", "status_code", resp.StatusCode)
	}
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		logging.Fatal("failed to read response body", "error", err)
	}
	registerDefinition := model.RegisterDefinition{}
	logging.Debug("received message", "body", string(body))
	err = json.Unmarshal(body, &registerDefinition)
	if err != nil {
		logging.Fatal("error unmarshalling master data", "error", err)
	}
	return &registerDefinition, nil
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
