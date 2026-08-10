package services

import (
	"fmt"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/persistence"
)

type RawKpiRegisterValues struct {
	JobId               string  `json:"jobId"`
	InverterActivePower float64 `json:"inverterActivePower"`
	MeterActivePower    float64 `json:"meterActivePower"`
	InverterDailyYield  float64 `json:"inverterDailyYield"`
	MeterExportedEnergy float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy float64 `json:"meterImportedEnergy"`
}

type MeterEnergyTodayResponse struct {
	ExportedEnergyToday float64 `json:"exportedEnergyToday"`
	ImportedEnergyToday float64 `json:"importedEnergyToday"`
}

// PersistenceService is a thin domain wrapper around the shared
// pvlogger-shared-modbus/persistence client (ADR-0010 error style).
type PersistenceService struct {
	client *persistence.PersistingService
}

func NewPersistenceService() *PersistenceService {
	cfg := sharedconfig.DataAccessorCfg()
	return &PersistenceService{
		client: persistence.NewPersistingService(cfg.Host, cfg.Port, cfg.Protocol, sharedconfig.DataAccessorBasePath),
	}
}

func (s *PersistenceService) StoreRawValues(values *RawKpiRegisterValues) error {
	logging.Info("storing raw KPI register values")
	return s.client.StoreValues(values, sharedconfig.StoreRawKpiRegisterValuesPath)
}

func (s *PersistenceService) StoreKpiValues(kpiValues *kpis.PvLoggerKpiData) error {
	logging.Info("storing KPI values")
	return s.client.StoreValues(kpiValues, sharedconfig.StoreKpiValuesPath)
}

func (s *PersistenceService) ReadMeterEnergyToday() (*MeterEnergyTodayResponse, error) {
	logging.Info("reading meter energy today")
	var result MeterEnergyTodayResponse
	if err := s.client.GetJSON(sharedconfig.ReadMeterEnergyTodayPath, &result); err != nil {
		return nil, fmt.Errorf("error reading meter energy today: %w", err)
	}
	logging.Info("meter energy today", "exported", result.ExportedEnergyToday, "imported", result.ImportedEnergyToday)
	return &result, nil
}
