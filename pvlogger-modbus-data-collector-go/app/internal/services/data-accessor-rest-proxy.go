package services

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strconv"
	"strings"
	"time"
)

type DataAccessorService struct {
	baseURL    string
	httpClient *http.Client
}

func NewDataAccessorService(config *config.DataAccessorConfig) *DataAccessorService {
	return &DataAccessorService{
		baseURL: fmt.Sprintf("%s://%s:%d", config.Protocol, config.Host, config.Port),
		httpClient: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

func InverterActivePower(jobId *model.JobId) *kpis.InverterActivePower {
	md, err := NewMasterDataService(config.MdConfig()).MasterDataForInverterActivePower()
	if err != nil {
		log.Default().Println("Error retrieving master data for inverter active power:", err)
		return kpis.NewInverterActivePower(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewInverterActivePower(
		retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func MeterActivePower(jobId *model.JobId) *kpis.MeterActivePower {
	md, err := NewMasterDataService(config.MdConfig()).MasterDataForSmartMeterActivePower()
	if err != nil {
		log.Default().Println("Error retrieving master data for smart meter active power:", err)
		return kpis.NewMeterActivePower(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewMeterActivePower(
		retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().MeterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func InverterEnergyToday(jobId *model.JobId) *kpis.InverterEnergyToday {
	md, err := NewMasterDataService(config.MdConfig()).MasterDataForInverterEnergyYield()
	if err != nil {
		log.Default().Println("Error retrieving master data for inverter energy yield:", err)
		return kpis.NewInverterEnergyToday(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewInverterEnergyToday(
		retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func MeterEnergyToday() *kpis.MeterEnergyToday {
	mdImported, err := NewMasterDataService(config.MdConfig()).MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return kpis.NewMeterEnergyToday(
			0.0,
			kpis.Unit{},
			0.0,
			kpis.Unit{},
		)
	}
	mdExported, err := NewMasterDataService(config.MdConfig()).MasterDataForMeterExportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter exported energy:", err)
		return kpis.NewMeterEnergyToday(
			0.0,
			kpis.Unit{},
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewMeterEnergyToday(
		computeImportedEnergyToday(config.DevIdConfig().MeterDeviceIds),
		kpis.ParseUnitFromString(mdImported.Unit),
		computeExportedEnergyToday(config.DevIdConfig().MeterDeviceIds),
		kpis.ParseUnitFromString(mdExported.Unit),
	)
}

func retrieveLatestRegisterValuesAggregated(masterData *model.RegisterDefinition, deviceIds []model.UByte, jobId *model.JobId) float64 {
	var sum = 0.0
	for _, deviceId := range deviceIds {
		sum += retrieveLatestRegisterValue(masterData, deviceId, jobId)
	}
	return sum
}

func retrieveLatestRegisterValue(registerDefinition *model.RegisterDefinition, deviceId uint8, jobId *model.JobId) float64 {
	pathSegments := []string{
		config.DataAccessorCfg().ReadLatestRegisterValuePath,
		registerDefinition.Type,
		strconv.Itoa(int(deviceId)),
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
		jobId.Id,
	}
	return retrieveValue(registerDefinition, pathSegments)
}

func retrieveEnergyTodaySoFar(registerDefinition *model.RegisterDefinition) float64 {
	pathSegments := []string{
		config.DataAccessorCfg().ReadKwTodaySoFarPath,
		registerDefinition.Type,
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
	}
	return retrieveValue(registerDefinition, pathSegments)
}

func retrieveValue(registerDefinition *model.RegisterDefinition, pathSegments []string) float64 {
	service := NewDataAccessorService(config.DataAccessorCfg())
	getUrl, err := url.Parse(fmt.Sprintf("%s/%s", service.baseURL, strings.Join(pathSegments, "/")))
	if err != nil {
		log.Default().Println("Error creating url for retrieving value:", err)
		return 0.0
	}
	log.Default().Println("Retrieving value from url: " + getUrl.String())
	response, err := service.httpClient.Get(getUrl.String())
	if err != nil {
		log.Default().Println("Error retrieving value:", err)
		return 0.0
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
		}
	}(response.Body)
	if response.StatusCode != http.StatusOK {
		log.Default().Println("Error retrieving value; response status is not OK: " + response.Status)
		return 0.0
	}
	log.Default().Println("Response status: " + response.Status)
	body, err := io.ReadAll(response.Body)
	if err != nil {
		log.Default().Println("Error reading response body:", err)
		return 0.0
	}
	log.Default().Println("Received message: " + string(body))
	value := toFloat64OrDefault(string(body), 0.0)
	gain := toFloat64OrDefault(kpis.NewGainFromString(registerDefinition.Gain).String(), 1.0)
	result := value / gain
	log.Default().Println("Value retrieved: " + fmt.Sprintf("%f", value))
	log.Default().Println("Gain(" + fmt.Sprintf("%s", registerDefinition.Gain) + "): " + fmt.Sprintf("%f", gain))
	log.Default().Println("Calculated value: " + fmt.Sprintf("%f", result))
	return result
}

func computeImportedEnergyToday(deviceIds []model.UByte) float64 {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return sumUp(deviceIds, md)
}

func computeExportedEnergyToday(deviceIds []model.UByte) float64 {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForMeterExportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return sumUp(deviceIds, md)
}

func sumUp(deviceIds []model.UByte, md *model.RegisterDefinition) float64 {
	var energyToday = 0.0
	for i := 0; i < len(deviceIds); i++ {
		energyToday += retrieveEnergyTodaySoFar(md)
	}
	return energyToday
}

func toFloat64OrDefault(text string, fallback float64) float64 {
	value, err := strconv.ParseFloat(text, 64)
	if err != nil {
		return fallback
	}
	return value
}
