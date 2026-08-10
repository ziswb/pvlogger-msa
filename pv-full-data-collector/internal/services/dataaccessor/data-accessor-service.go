package dataaccessor

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/model"
	"strconv"
	"strings"
)

func (s *DataAccessorService) InverterActivePower(jobId *model.JobId) (*kpis.InverterActivePower, error) {
	md, err := s.masterDataService.MasterDataForInverterActivePower()
	if err != nil {
		return nil, fmt.Errorf("error retrieving master data for inverter active power: %w", err)
	}
	value, err := s.retrieveLatestRegisterValuesAggregated(md, sharedconfig.DevIdConfig().InverterDeviceIds, jobId)
	if err != nil {
		return nil, err
	}
	return kpis.NewInverterActivePower(value, kpis.ParseUnitFromString(md.Unit)), nil
}

func (s *DataAccessorService) MeterActivePower(jobId *model.JobId) (*kpis.MeterActivePower, error) {
	md, err := s.masterDataService.MasterDataForSmartMeterActivePower()
	if err != nil {
		return nil, fmt.Errorf("error retrieving master data for smart meter active power: %w", err)
	}
	value, err := s.retrieveLatestRegisterValuesAggregated(md, sharedconfig.DevIdConfig().MeterDeviceIds, jobId)
	if err != nil {
		return nil, err
	}
	return kpis.NewMeterActivePower(value, kpis.ParseUnitFromString(md.Unit)), nil
}

func (s *DataAccessorService) InverterEnergyToday(jobId *model.JobId) (*kpis.InverterEnergyToday, error) {
	md, err := s.masterDataService.MasterDataForInverterEnergyYield()
	if err != nil {
		return nil, fmt.Errorf("error retrieving master data for inverter energy yield: %w", err)
	}
	value, err := s.retrieveLatestRegisterValuesAggregated(md, sharedconfig.DevIdConfig().InverterDeviceIds, jobId)
	if err != nil {
		return nil, err
	}
	return kpis.NewInverterEnergyToday(value, kpis.ParseUnitFromString(md.Unit)), nil
}

func (s *DataAccessorService) MeterEnergyToday() (*kpis.MeterEnergyToday, error) {
	mdImported, err := s.masterDataService.MasterDataForMeterImportedEnergy()
	if err != nil {
		return nil, fmt.Errorf("error retrieving master data for meter imported energy: %w", err)
	}
	mdExported, err := s.masterDataService.MasterDataForMeterExportedEnergy()
	if err != nil {
		return nil, fmt.Errorf("error retrieving master data for meter exported energy: %w", err)
	}
	imported, err := s.retrieveEnergyTodaySoFar(mdImported)
	if err != nil {
		return nil, err
	}
	exported, err := s.retrieveEnergyTodaySoFar(mdExported)
	if err != nil {
		return nil, err
	}
	return kpis.NewMeterEnergyToday(
		imported,
		kpis.ParseUnitFromString(mdImported.Unit),
		exported,
		kpis.ParseUnitFromString(mdExported.Unit),
	), nil
}

func (s *DataAccessorService) retrieveLatestRegisterValuesAggregated(masterData *model.RegisterDefinition, deviceIds []model.UByte, jobId *model.JobId) (float64, error) {
	var sum = 0.0
	for _, deviceId := range deviceIds {
		value, err := s.retrieveLatestRegisterValue(masterData, deviceId, jobId)
		if err != nil {
			return 0, err
		}
		sum += value
	}
	return sum, nil
}

func (s *DataAccessorService) retrieveLatestRegisterValue(registerDefinition *model.RegisterDefinition, deviceId uint8, jobId *model.JobId) (float64, error) {
	pathSegments := []string{
		sharedconfig.ReadLatestRegisterValuePath,
		registerDefinition.Type,
		strconv.Itoa(int(deviceId)),
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
		jobId.Id,
	}
	return s.retrieveValue(registerDefinition, pathSegments)
}

func (s *DataAccessorService) retrieveEnergyTodaySoFar(registerDefinition *model.RegisterDefinition) (float64, error) {
	pathSegments := []string{
		sharedconfig.ReadKwTodaySoFarPath,
		registerDefinition.Type,
		strconv.Itoa(registerDefinition.RegisterAddress.Value),
	}
	return s.retrieveValue(registerDefinition, pathSegments)
}

func (s *DataAccessorService) retrieveValue(registerDefinition *model.RegisterDefinition, pathSegments []string) (float64, error) {
	getUrl, err := url.Parse(fmt.Sprintf("%s/%s", s.baseURL, strings.Join(pathSegments, "/")))
	if err != nil {
		return 0, fmt.Errorf("error creating url for retrieving value: %w", err)
	}
	if logging.DebugEnabled() {
		logging.Debug("retrieving value", "url", getUrl.String())
	}
	response, err := s.httpClient.Get(getUrl.String())
	if err != nil {
		return 0, fmt.Errorf("error retrieving value: %w", err)
	}
	defer s.close(response)
	if response.StatusCode != http.StatusOK {
		return 0, fmt.Errorf("error retrieving value: unexpected status %s", response.Status)
	}
	if logging.DebugEnabled() {
		logging.Debug("response received", "status", response.Status)
	}
	body, err := io.ReadAll(response.Body)
	if err != nil {
		return 0, fmt.Errorf("error reading response body: %w", err)
	}
	if logging.DebugEnabled() {
		logging.Debug("received message", "body", string(body))
	}
	value := toFloat64OrDefault(string(body), 0.0)
	gain := toFloat64OrDefault(kpis.NewGainFromString(registerDefinition.Gain).String(), 1.0)
	result := value / gain
	if logging.DebugEnabled() {
		logging.Debug("value calculated", "value", value, "gain", gain, "result", result)
	}
	return result, nil
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
