package masterdata

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strings"
)

func (s *MasterDataService) FetchRegisterDefinitions(
	deviceTypes []model.DeviceType,
	categories []model.Category,
) (*[]model.RegisterDefinition, error) {
	log.Default().Println("Fetching master data ...")
	devTypesParam := strings.Join(arrayOfDeviceTypesToArrayOfStrings(deviceTypes), paramValueSeparator)
	categoriesParam := strings.Join(arrayOfCategoriesToArrayOfStrings(categories), paramValueSeparator)

	reqURL, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, config.MdConfig().MasterDataPath))
	if err != nil {
		log.Default().Fatal("failed to parse URL", err)
	}

	q := reqURL.Query()
	q.Set(paramDevTypes, devTypesParam)
	q.Set(paramCategories, categoriesParam)
	reqURL.RawQuery = q.Encode()

	log.Default().Printf("Fetching master data from %s", reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	defer s.close(resp)
	if err != nil {
		log.Default().Fatal("failed to fetch master data", err)
	}

	log.Default().Printf("Response status: %s", resp.Status)
	if resp.StatusCode != http.StatusOK {
		log.Default().Fatalf("unexpected status code: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Default().Fatal("failed to read response body", err)
	}
	var definitions []model.RegisterDefinition
	log.Default().Println("Received message: " + string(body))
	if err := json.Unmarshal(body, &definitions); err != nil {
		log.Default().Fatal("failed to unmarshal response", err)
	}

	log.Default().Println("Fetched master data successfully.")
	return &definitions, nil
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
		log.Default().Fatal("failed to parse URL", err)
	}
	log.Default().Printf("Retrieving master data for register address: %s", reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	defer s.close(resp)
	if err != nil {
		log.Default().Fatal("Error retrieving master data", err)
	}
	log.Default().Printf("Response status: %s", resp.Status)
	if resp.StatusCode != http.StatusOK {
		log.Default().Fatalf("unexpected status code: %d", resp.StatusCode)
	}
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Default().Fatalf("failed to read response body: %v", err)
	}
	registerDefinition := model.RegisterDefinition{}
	log.Default().Printf("Received message: %s", string(body))
	err = json.Unmarshal(body, &registerDefinition)
	if err != nil {
		log.Default().Fatal("Error unmarshalling master data", err)
	}
	return &registerDefinition, nil
}

func (s *MasterDataService) close(response *http.Response) {
	if response != nil && response.Body != nil {
		err := response.Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
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
