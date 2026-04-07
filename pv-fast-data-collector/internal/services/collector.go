package services

import (
	"fmt"
	"pv-fast-data-collector/internal/registers"
	"pv-fast-data-collector/internal/services/metrics"
	modbus "pvlogger-shared-modbus"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/request"
	"pvlogger-shared-modbus/response"

	"pv-logger/logging"
	"strconv"
	"time"

	"github.com/google/uuid"
)

func CollectAndComputeKpis() {
	jobId := uuid.New().String()
	logging.Info("starting collection", "job_id", jobId)

	rawValues := readModbusRegisters(jobId)
	logging.Info("raw values collected",
		"inverter_active_power", rawValues.InverterActivePower,
		"meter_active_power", rawValues.MeterActivePower,
		"inverter_daily_yield", rawValues.InverterDailyYield,
		"meter_exported_energy", rawValues.MeterExportedEnergy,
		"meter_imported_energy", rawValues.MeterImportedEnergy,
	)

	persistence := NewPersistenceService()

	// Store raw values
	persistence.StoreRawValues(&RawKpiRegisterValues{
		JobId:               jobId,
		InverterActivePower: rawValues.InverterActivePower,
		MeterActivePower:    rawValues.MeterActivePower,
		InverterDailyYield:  rawValues.InverterDailyYield,
		MeterExportedEnergy: rawValues.MeterExportedEnergy,
		MeterImportedEnergy: rawValues.MeterImportedEnergy,
	})

	// Read meter energy today (delta computation from data accessor)
	meterEnergyToday := persistence.ReadMeterEnergyToday()

	// Compute KPIs
	liveKpis := computeLiveKpis(rawValues)
	todayKpis := computeTodayKpis(rawValues, meterEnergyToday)

	kpiValues := kpis.PvLoggerKpiData{
		JobId:         jobId,
		DateCreated:   time.Now().UnixMilli(),
		Producing:     liveKpis.Producing(),
		Exporting:     liveKpis.Exporting(),
		Importing:     liveKpis.Importing(),
		Consuming:     liveKpis.Consuming(),
		SelfConsuming: liveKpis.SelfConsuming(),
		Produced:      todayKpis.Produced().ToWh(),
		Exported:      todayKpis.Exported().ToWh(),
		Imported:      todayKpis.Imported().ToWh(),
		Consumed:      todayKpis.Consumed().ToWh(),
		SelfConsumed:  todayKpis.SelfConsumed().ToWh(),
	}

	persistence.StoreKpiValues(&kpiValues)
	pushKpiMetrics(&kpiValues)
	logging.Info("KPIs computed and stored", "job_id", jobId)
}

type collectedRawValues struct {
	InverterActivePower float64
	MeterActivePower    float64
	InverterDailyYield  float64
	MeterExportedEnergy float64
	MeterImportedEnergy float64
}

func readModbusRegisters(jobId string) *collectedRawValues {
	cfg := sharedconfig.ModbusConfig()
	devCfg := sharedconfig.DevIdConfig()
	address := fmt.Sprintf("%s:%s", cfg.Host, strconv.Itoa(cfg.Port))

	handler := modbus.NewTcpHandler(address, cfg.Timeout, cfg.ConnectionSleep)
	if err := handler.Connect(); err != nil {
		logging.Fatal("failed to connect to modbus device", "error", err)
	}
	defer func() {
		if err := handler.Close(); err != nil {
			logging.Error("failed to close modbus connection", "error", err)
		}
	}()

	var requestCounter model.UShort = 1
	result := &collectedRawValues{}

	// Read inverter registers for each inverter device
	for _, deviceId := range devCfg.InverterDeviceIds {
		logging.Info("reading inverter registers", "device_id", deviceId)

		// Inverter active power (U32) - read separately
		activePower := readRegisters(handler, jobId, &requestCounter, deviceId, registers.InverterActivePowerRegisters())
		result.InverterActivePower += applyGain(activePower, registers.InverterActivePower)

		// Inverter daily yield (U32) - read separately
		dailyYield := readRegisters(handler, jobId, &requestCounter, deviceId, registers.InverterDailyYieldRegisters())
		result.InverterDailyYield += applyGain(dailyYield, registers.InverterDailyYield)
	}

	// Read meter registers for each meter device
	for _, deviceId := range devCfg.MeterDeviceIds {
		logging.Info("reading meter registers", "device_id", deviceId)

		// Meter active power (I32) - read separately
		meterPowerValues := readRegisters(handler, jobId, &requestCounter, deviceId, registers.MeterActivePowerRegisters())
		result.MeterActivePower += applyGain(meterPowerValues, registers.MeterActivePower)

		// Meter energy registers (consecutive U32 block: 37119 + 37121)
		meterEnergyValues := readRegisters(handler, jobId, &requestCounter, deviceId, registers.MeterEnergyRegisters())
		result.MeterExportedEnergy += applyGain(meterEnergyValues, registers.MeterExportedEnergy)
		result.MeterImportedEnergy += applyGain(meterEnergyValues, registers.MeterImportedEnergy)
	}

	return result
}

func readRegisters(handler *modbus.TcpHandler, jobId string, requestCounter *model.UShort, deviceId uint8, regDefs []model.RegisterDefinition) []*response.RegisterValue {
	req := &request.ReadHoldingRegistersValueRequest{
		JobId:               model.NewJobId(jobId),
		RequestTime:         time.Now(),
		RequestId:           model.NewRequestId(*requestCounter),
		LogicDeviceId:       deviceId,
		RegisterDefinitions: regDefs,
	}
	*requestCounter++

	header, pduBytes, err := handler.ExecuteRequest(req.EncodeToBytes())
	if err != nil {
		logging.Fatal("failed to execute modbus request", "error", err)
	}

	resp := response.NewRegisterValueResponseFromBytes(regDefs, req.JobId, req.RequestId, req.RequestTime, *header, pduBytes)
	return resp.Values
}

func applyGain(values []*response.RegisterValue, targetDef model.RegisterDefinition) float64 {
	for _, v := range values {
		if v.Definition.RegisterAddress.Value == targetDef.RegisterAddress.Value {
			rawValue := toFloat64(v.Value)
			gain := toFloat64FromGain(targetDef.Gain)
			result := rawValue / gain
			if logging.DebugEnabled() {
				logging.Debug("gain applied", "register", targetDef.Name, "raw", rawValue, "gain", gain, "result", result)
			}
			return result
		}
	}
	logging.Warn("register value not found", "register", targetDef.Name, "address", targetDef.RegisterAddress.Value)
	return 0.0
}

func toFloat64(value any) float64 {
	switch v := value.(type) {
	case uint32:
		return float64(v)
	case int32:
		return float64(v)
	default:
		logging.Warn("unexpected value type", "value", value)
		return 0.0
	}
}

func toFloat64FromGain(gainStr string) float64 {
	gain := kpis.NewGainFromString(gainStr)
	value, err := strconv.ParseFloat(gain.String(), 64)
	if err != nil {
		return 1.0
	}
	return value
}

func computeLiveKpis(raw *collectedRawValues) *kpis.KpiLiveData {
	inverterPower := kpis.NewInverterActivePower(raw.InverterActivePower, kpis.ParseUnitFromString("kW"))
	meterPower := kpis.NewMeterActivePower(raw.MeterActivePower, kpis.ParseUnitFromString("W"))
	return &kpis.KpiLiveData{
		InverterActivePower: inverterPower,
		MeterActivePower:    meterPower,
	}
}

func pushKpiMetrics(kpiValues *kpis.PvLoggerKpiData) {
	cfg := sharedconfig.PushGatewayCfg()
	if !cfg.Enabled {
		logging.Debug("push gateway is disabled")
		return
	}
	metrics.NewMetricsPushService(cfg).PushKpiMetrics(kpiValues)
}

func computeTodayKpis(raw *collectedRawValues, meterEnergy *MeterEnergyTodayResponse) *kpis.KpiTodayData {
	inverterEnergy := kpis.NewInverterEnergyToday(raw.InverterDailyYield, kpis.ParseUnitFromString("kWh"))
	meterEnergyToday := kpis.NewMeterEnergyToday(
		meterEnergy.ImportedEnergyToday, kpis.ParseUnitFromString("kWh"),
		meterEnergy.ExportedEnergyToday, kpis.ParseUnitFromString("kWh"),
	)
	return &kpis.KpiTodayData{
		InverterTodayEnergy: inverterEnergy,
		MeterTodayEnergy:    meterEnergyToday,
	}
}
