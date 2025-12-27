package services

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
	"time"
)

const (
	paramDevTypes       = "deviceTypes"
	paramCategories     = "categories"
	paramValueSeparator = ";"
)

var (
	smartMeterActivePowerRegisterAddress    = model.RegisterAddress{Value: 37113} // -> W
	inverterActivePowerRegisterAddress      = model.RegisterAddress{Value: 32080} // -> kW
	smartMeterExportedEnergyRegisterAddress = model.RegisterAddress{Value: 37119} // -> kWh
	smartMeterImportedEnergyRegisterAddress = model.RegisterAddress{Value: 37121} // -> kWh
	inverterDailyEnergyYieldRegisterAddress = model.RegisterAddress{Value: 32114} // -> kWh
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
) (*[]model.ModbusRegisterDefinition, error) {
	log.Default().Println("Fetching master data ...")
	devTypesParam := strings.Join(deviceTypes, paramValueSeparator)
	categoriesParam := strings.Join(categories, paramValueSeparator)

	reqURL, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, config.MdConfig().MasterDataPath))
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL: %w", err)
	}

	q := reqURL.Query()
	q.Set(paramDevTypes, devTypesParam)
	q.Set(paramCategories, categoriesParam)
	reqURL.RawQuery = q.Encode()

	log.Default().Println("Fetching master data from " + reqURL.String())
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
	var definitions []model.ModbusRegisterDefinition
	log.Default().Println("Received message: " + string(body))
	if err := json.Unmarshal(body, &definitions); err != nil {
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	log.Default().Println("Fetched master data successfully.")
	return &definitions, nil
}

func (s *MasterDataService) MasterDataForSmartMeterActivePower() (*model.ModbusRegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterActivePowerRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForInverterActivePower() (*model.ModbusRegisterDefinition, error) {
	return s.fetchMasterDataFor(inverterActivePowerRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForInverterEnergyYield() (*model.ModbusRegisterDefinition, error) {
	return s.fetchMasterDataFor(inverterDailyEnergyYieldRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForMeterExportedEnergy() (*model.ModbusRegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterExportedEnergyRegisterAddress.Value)
}

func (s *MasterDataService) MasterDataForMeterImportedEnergy() (*model.ModbusRegisterDefinition, error) {
	return s.fetchMasterDataFor(smartMeterImportedEnergyRegisterAddress.Value)
}

func (s *MasterDataService) fetchMasterDataFor(registerAddress int) (*model.ModbusRegisterDefinition, error) {
	baseURL := s.baseURL
	masterDataPath := config.MdConfig().MasterDataPath
	reqURL, err := url.Parse(fmt.Sprintf("%s/%s/%d", baseURL, masterDataPath, registerAddress))
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL: %w", err)
	}
	log.Default().Println("Retrieving master data for register address: " + reqURL.String())
	resp, err := s.httpClient.Get(reqURL.String())
	if err != nil {
		return nil, err
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
	registerDefinition := model.ModbusRegisterDefinition{}
	log.Default().Println("Received message: " + string(body))
	err = json.Unmarshal(body, &registerDefinition)
	if err != nil {
		return nil, err
	}
	return &registerDefinition, nil
}
