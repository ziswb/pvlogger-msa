package services

import (
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/persistence"
	"pvlogger-shared-modbus/response"
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

// StoreModbusValues stores a register-value response in the data accessor's
// generic register store — the same store the full collector's sweep fills.
// The shared KPI aggregation reads the dongle block back from it per jobId.
func (s *PersistenceService) StoreModbusValues(values *response.RegisterValueResponse) error {
	logging.Info("storing dongle block register values")
	return s.client.StoreValues(values, sharedconfig.StoreModbusValuesPath)
}
