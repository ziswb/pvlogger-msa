package dataaccessor

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
)

func (s *DataAccessorService) InverterActivePower(jobId *model.JobId) *kpis.InverterActivePower {
	md, err := s.masterDataService.MasterDataForInverterActivePower()
	if err != nil {
		log.Default().Println("Error retrieving master data for inverter active power:", err)
		return kpis.NewInverterActivePower(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewInverterActivePower(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) MeterActivePower(jobId *model.JobId) *kpis.MeterActivePower {
	md, err := s.masterDataService.MasterDataForSmartMeterActivePower()
	if err != nil {
		log.Default().Println("Error retrieving master data for smart meter active power:", err)
		return kpis.NewMeterActivePower(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewMeterActivePower(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().MeterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) InverterEnergyToday(jobId *model.JobId) *kpis.InverterEnergyToday {
	md, err := s.masterDataService.MasterDataForInverterEnergyYield()
	if err != nil {
		log.Default().Println("Error retrieving master data for inverter energy yield:", err)
		return kpis.NewInverterEnergyToday(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewInverterEnergyToday(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) MeterEnergyToday() *kpis.MeterEnergyToday {
	mdImported, err := s.masterDataService.MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return kpis.NewMeterEnergyToday(
			0.0,
			kpis.Unit{},
			0.0,
			kpis.Unit{},
		)
	}
	mdExported, err := s.masterDataService.MasterDataForMeterExportedEnergy()
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
		s.computeImportedEnergyToday(config.DevIdConfig().MeterDeviceIds),
		kpis.ParseUnitFromString(mdImported.Unit),
		s.computeExportedEnergyToday(config.DevIdConfig().MeterDeviceIds),
		kpis.ParseUnitFromString(mdExported.Unit),
	)
}

func (s *DataAccessorService) retrieveLatestRegisterValuesAggregated(masterData *model.RegisterDefinition, deviceIds []model.UByte, jobId *model.JobId) float64 {
	var sum = 0.0
	for _, deviceId := range deviceIds {
		sum += s.retrieveLatestRegisterValue(masterData, deviceId, jobId)
	}
	return sum
}

func (s *DataAccessorService) retrieveLatestRegisterValue(registerDefinition *model.RegisterDefinition, deviceId uint8, jobId *model.JobId) float64 {
	pathSegments := []string{
		config.DataAccessorCfg().ReadLatestRegisterValuePath,
		registerDefinition.Type,
		strconv.Itoa(int(deviceId)),
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
		jobId.Id,
	}
	return s.retrieveValue(registerDefinition, pathSegments)
}

func (s *DataAccessorService) retrieveEnergyTodaySoFar(registerDefinition *model.RegisterDefinition) float64 {
	pathSegments := []string{
		config.DataAccessorCfg().ReadKwTodaySoFarPath,
		registerDefinition.Type,
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
	}
	return s.retrieveValue(registerDefinition, pathSegments)
}

func (s *DataAccessorService) retrieveValue(registerDefinition *model.RegisterDefinition, pathSegments []string) float64 {
	getUrl, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, strings.Join(pathSegments, "/")))
	if err != nil {
		log.Default().Println("Error creating url for retrieving value:", err)
		return 0.0
	}
	log.Default().Println("Retrieving value from url: " + getUrl.String())
	response, err := s.httpClient.Get(getUrl.String())
	defer s.close(response)
	if err != nil {
		log.Default().Println("Error retrieving value:", err)
		return 0.0
	}
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

func (s *DataAccessorService) computeImportedEnergyToday(deviceIds []model.UByte) float64 {
	md, err := s.masterDataService.MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return s.sumUp(deviceIds, md)
}

func (s *DataAccessorService) computeExportedEnergyToday(deviceIds []model.UByte) float64 {
	md, err := s.masterDataService.MasterDataForMeterExportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return s.sumUp(deviceIds, md)
}

func (s *DataAccessorService) sumUp(deviceIds []model.UByte, md *model.RegisterDefinition) float64 {
	var energyToday = 0.0
	for i := 0; i < len(deviceIds); i++ {
		energyToday += s.retrieveEnergyTodaySoFar(md)
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

func (s *DataAccessorService) close(response *http.Response) {
	if response != nil && response.Body != nil {
		err := response.Body.Close()
		if err != nil {
			log.Default().Println("Error closing response body:", err)
		}
	}
}
