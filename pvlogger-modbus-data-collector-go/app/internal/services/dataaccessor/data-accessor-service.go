package dataaccessor

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/logging"
	"pvlogger-modbus-data-collector-go/internal/modbus/model"
	"strconv"
	"strings"
)

func (s *DataAccessorService) InverterActivePower(jobId *model.JobId) *kpis.InverterActivePower {
	md, err := s.masterDataService.MasterDataForInverterActivePower()
	if err != nil {
		logging.Fatal("error retrieving master data for inverter active power", "error", err)
	}
	return kpis.NewInverterActivePower(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) MeterActivePower(jobId *model.JobId) *kpis.MeterActivePower {
	md, err := s.masterDataService.MasterDataForSmartMeterActivePower()
	if err != nil {
		logging.Fatal("error retrieving master data for smart meter active power", "error", err)
	}
	return kpis.NewMeterActivePower(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().MeterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) InverterEnergyToday(jobId *model.JobId) *kpis.InverterEnergyToday {
	md, err := s.masterDataService.MasterDataForInverterEnergyYield()
	if err != nil {
		logging.Fatal("error retrieving master data for inverter energy yield", "error", err)
	}
	return kpis.NewInverterEnergyToday(
		s.retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().InverterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func (s *DataAccessorService) MeterEnergyToday() *kpis.MeterEnergyToday {
	mdImported, err := s.masterDataService.MasterDataForMeterImportedEnergy()
	if err != nil {
		logging.Fatal("error retrieving master data for meter imported energy", "error", err)
	}
	mdExported, err := s.masterDataService.MasterDataForMeterExportedEnergy()
	if err != nil {
		logging.Fatal("error retrieving master data for meter exported energy", "error", err)
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
		logging.Fatal("error creating url for retrieving value", "error", err)
	}
	logging.Debug("retrieving value", "url", getUrl.String())
	response, err := s.httpClient.Get(getUrl.String())
	defer s.close(response)
	if err != nil {
		logging.Fatal("error retrieving value", "error", err)
	}
	if response.StatusCode != http.StatusOK {
		logging.Fatal("error retrieving value", "status", response.Status)
	}
	logging.Debug("response received", "status", response.Status)
	body, err := io.ReadAll(response.Body)
	if err != nil {
		logging.Fatal("error reading response body", "error", err)
	}
	logging.Debug("received message", "body", string(body))
	value := toFloat64OrDefault(string(body), 0.0)
	gain := toFloat64OrDefault(kpis.NewGainFromString(registerDefinition.Gain).String(), 1.0)
	result := value / gain
	logging.Debug("value calculated", "value", value, "gain", gain, "result", result)
	return result
}

func (s *DataAccessorService) computeImportedEnergyToday(deviceIds []model.UByte) float64 {
	md, err := s.masterDataService.MasterDataForMeterImportedEnergy()
	if err != nil {
		logging.Fatal("error retrieving master data for meter imported energy", "error", err)
	}
	return s.sumUp(deviceIds, md)
}

func (s *DataAccessorService) computeExportedEnergyToday(deviceIds []model.UByte) float64 {
	md, err := s.masterDataService.MasterDataForMeterExportedEnergy()
	if err != nil {
		logging.Fatal("error retrieving master data for meter exported energy", "error", err)
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
			logging.Error("error closing response body", "error", err)
		}
	}
}
