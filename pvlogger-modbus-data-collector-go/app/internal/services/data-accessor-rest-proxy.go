package services

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"pvlogger-modbus-data-collector-go/internal/config"
	"pvlogger-modbus-data-collector-go/internal/kpis"
	"pvlogger-modbus-data-collector-go/internal/modbus"
	"strconv"
)

func InverterActivePower(jobId *modbus.JobId) *kpis.InverterActivePower {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForInverterActivePower()
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

func MeterActivePower(jobId *modbus.JobId) *kpis.MeterActivePower {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForSmartMeterActivePower()
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

func InverterEnergyToday(jobId *modbus.JobId) *kpis.InverterEnergyToday {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForInverterEnergyYield()
	if err != nil {
		log.Default().Println("Error retrieving master data for inverter energy yield:", err)
		return kpis.NewInverterEnergyToday(
			0.0,
			kpis.Unit{},
		)
	}
	return kpis.NewInverterEnergyToday(
		retrieveLatestRegisterValuesAggregated(md, config.DevIdConfig().MeterDeviceIds, jobId),
		kpis.ParseUnitFromString(md.Unit),
	)
}

func MeterEnergyToday() *kpis.MeterEnergyToday {
	mdSvc := NewMasterDataService(config.MdConfig())
	mdImported, err := mdSvc.MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return kpis.NewMeterEnergyToday(
			0.0,
			kpis.Unit{},
			0.0,
			kpis.Unit{},
		)
	}
	mdExported, err := mdSvc.MasterDataForMeterExportedEnergy()
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

func retrieveLatestRegisterValuesAggregated(masterData *modbus.ModbusRegisterDefinition, deviceIds []modbus.UByte, jobId *modbus.JobId) float64 {
	var sum = 0.0
	for _, deviceId := range deviceIds {
		sum += retrieveLatestRegisterValue(masterData, deviceId, jobId)
	}
	return sum
}

func retrieveLatestRegisterValue(registerDefinition *modbus.ModbusRegisterDefinition, deviceId uint8, jobId *modbus.JobId) float64 {
	host := config.DataAccessorCfg().Host
	port := config.DataAccessorCfg().Port
	protocol := config.DataAccessorCfg().Protocol
	readLatestRegisterValuePath := config.DataAccessorCfg().ReadLatestRegisterValuePath
	getUrl, err := url.Parse(
		fmt.Sprintf(
			"%s://%s:%d/%s/%s/%d/%d/%s",
			protocol,
			host,
			port,
			readLatestRegisterValuePath,
			registerDefinition.Type,
			deviceId,
			registerDefinition.RegisterAddress.Value,
			jobId.Id,
		),
	)
	if err != nil {
		log.Default().Println("Error creating url for latest register value:", err)
		return 0.0
	}
	log.Default().Println("Fetching latest register value from " + getUrl.String())
	response, err := http.Get(getUrl.String())
	if err != nil {
		log.Default().Println("Error retrieving latest register value:", err)
		return 0.0
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
		}
	}(response.Body)
	if response.StatusCode != http.StatusOK {
		log.Default().Println("Error retrieving latest register value: response status is not OK: " + response.Status)
		return 0.0
	}
	log.Default().Println("Response status: " + response.Status)
	body, err := io.ReadAll(response.Body)
	if err != nil {
		log.Default().Println("Error reading latest register value response body:", err)
		return 0.0
	}
	log.Default().Println("Received message: " + string(body))
	latestRegisterValue := toFloat64OrDefault(string(body), 0.0)
	gain := toFloat64OrDefault(registerDefinition.Gain, 1.0)
	return latestRegisterValue / gain
}

func retrieveEnergyTodaySoFar(registerDefinition *modbus.ModbusRegisterDefinition) float64 {
	host := config.DataAccessorCfg().Host
	port := config.DataAccessorCfg().Port
	protocol := config.DataAccessorCfg().Protocol
	readLatestRegisterValuePath := config.DataAccessorCfg().ReadKwTodaySoFarPath
	getUrl, err := url.Parse(
		fmt.Sprintf(
			"%s://%s:%d/%s/%s/%d",
			protocol,
			host,
			port,
			readLatestRegisterValuePath,
			registerDefinition.Type,
			registerDefinition.RegisterAddress.Value,
		),
	)
	if err != nil {
		log.Default().Println("Error creating url for energy today so far:", err)
		return 0.0
	}
	log.Default().Println("Retrieving energy today so far from url: " + getUrl.String())
	response, err := http.Get(getUrl.String())
	if err != nil {
		log.Default().Println("Error retrieving energy today so far:", err)
		return 0.0
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Default().Println(err.Error())
		}
	}(response.Body)
	if response.StatusCode != http.StatusOK {
		log.Default().Println("Error retrieving energy today so far: response status is not OK: " + response.Status)
		return 0.0
	}
	log.Default().Println("Response status: " + response.Status)
	body, err := io.ReadAll(response.Body)
	if err != nil {
		log.Default().Println("Error reading energy today so far response body:", err)
		return 0.0
	}
	log.Default().Println("Received message: " + string(body))
	energyTodaySoFar := toFloat64OrDefault(string(body), 0.0)
	return energyTodaySoFar
}

func computeImportedEnergyToday(deviceIds []modbus.UByte) float64 {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForMeterImportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return sumUp(deviceIds, md)
}

func computeExportedEnergyToday(deviceIds []modbus.UByte) float64 {
	mdSvc := NewMasterDataService(config.MdConfig())
	md, err := mdSvc.MasterDataForMeterExportedEnergy()
	if err != nil {
		log.Default().Println("Error retrieving master data for meter imported energy:", err)
		return 0.0
	}
	return sumUp(deviceIds, md)
}

func sumUp(deviceIds []modbus.UByte, md *modbus.ModbusRegisterDefinition) float64 {
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
