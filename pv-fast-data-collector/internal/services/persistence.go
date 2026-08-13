package services

import (
	"fmt"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/persistence"
)

type RawKpiRegisterValues struct {
	JobId                    string  `json:"jobId"`
	InverterActivePower      float64 `json:"inverterActivePower"`
	MeterActivePower         float64 `json:"meterActivePower"`
	InverterDailyYield       float64 `json:"inverterDailyYield"`
	InverterAccumulatedYield float64 `json:"inverterAccumulatedYield"`
	MeterExportedEnergy      float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy      float64 `json:"meterImportedEnergy"`
}

type EnergyTodayResponse struct {
	ExportedEnergyToday float64 `json:"exportedEnergyToday"`
	ImportedEnergyToday float64 `json:"importedEnergyToday"`
	// nil while the data accessor has no lifetime-yield baseline for yesterday
	// (first day after the V9 rollout) — callers fall back to register 32114.
	ProducedEnergyToday *float64 `json:"producedEnergyToday"`
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

func (s *PersistenceService) ReadEnergyToday() (*EnergyTodayResponse, error) {
	logging.Info("reading energy today")
	var result EnergyTodayResponse
	if err := s.client.GetJSON(sharedconfig.ReadMeterEnergyTodayPath, &result); err != nil {
		return nil, fmt.Errorf("error reading energy today: %w", err)
	}
	logging.Info("energy today",
		"exported", result.ExportedEnergyToday,
		"imported", result.ImportedEnergyToday,
		"produced", result.ProducedEnergyToday,
	)
	return &result, nil
}
