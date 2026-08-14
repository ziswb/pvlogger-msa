package services

import (
	"fmt"
	"pv-fast-data-collector/internal/registers"
	modbus "pvlogger-shared-modbus"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/metrics"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/request"
	"pvlogger-shared-modbus/response"

	"pv-logger/logging"
	"strconv"
	"time"

	"github.com/google/uuid"
)

func CollectAndComputeKpis() error {
	jobId := uuid.New().String()
	logging.Info("starting collection", "job_id", jobId)

	rawValues, err := readModbusRegisters(jobId)
	if err != nil {
		return err
	}
	logging.Info("raw values collected",
		"inverter_active_power", rawValues.InverterActivePower,
		"meter_active_power", rawValues.MeterActivePower,
		"load_power", rawValues.LoadPower,
		"battery_power", rawValues.BatteryPower,
		"inverter_daily_yield", rawValues.InverterDailyYield,
		"inverter_accumulated_yield", rawValues.InverterAccumulatedYield,
		"meter_exported_energy", rawValues.MeterExportedEnergy,
		"meter_imported_energy", rawValues.MeterImportedEnergy,
	)

	persistence := NewPersistenceService()

	// Store raw values
	if err := persistence.StoreRawValues(&RawKpiRegisterValues{
		JobId:                    jobId,
		InverterActivePower:      rawValues.InverterActivePower,
		MeterActivePower:         rawValues.MeterActivePower,
		InverterDailyYield:       rawValues.InverterDailyYield,
		InverterAccumulatedYield: rawValues.InverterAccumulatedYield,
		MeterExportedEnergy:      rawValues.MeterExportedEnergy,
		MeterImportedEnergy:      rawValues.MeterImportedEnergy,
	}); err != nil {
		return err
	}

	// Read energy today (delta computation from data accessor; must run after
	// StoreRawValues — the deltas are anchored on the row stored above)
	energyToday, err := persistence.ReadEnergyToday()
	if err != nil {
		return err
	}

	// Compute KPIs
	liveKpis := computeLiveKpis(rawValues)
	todayKpis := computeTodayKpis(rawValues, energyToday)

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

	if err := persistence.StoreKpiValues(&kpiValues); err != nil {
		return err
	}
	pushKpiMetrics(&kpiValues)
	logging.Info("KPIs computed and stored", "job_id", jobId)
	return nil
}

type collectedRawValues struct {
	// The live power values come from the SmartDongle's aggregate block
	// (backlog 33) — one request, one coherent snapshot. The field names and
	// conventions are kept: InverterActivePower is the AC total in kW
	// (register 37516, previously 32080 summed per inverter), MeterActivePower
	// is W with positive = export (register 37502 sign-flipped, previously
	// 37113). LoadPower (37500, W) is the directly measured house consumption.
	InverterActivePower      float64
	MeterActivePower         float64
	LoadPower                float64
	BatteryPower             float64
	InverterDailyYield       float64
	InverterAccumulatedYield float64
	MeterExportedEnergy      float64
	MeterImportedEnergy      float64
}

func readModbusRegisters(jobId string) (*collectedRawValues, error) {
	cfg := sharedconfig.ModbusConfig()
	devCfg := sharedconfig.DevIdConfig()
	address := fmt.Sprintf("%s:%s", cfg.Host, strconv.Itoa(cfg.Port))

	handler := modbus.NewTcpHandler(address, cfg.Timeout, cfg.ConnectionSleep)
	if err := handler.Connect(); err != nil {
		return nil, fmt.Errorf("failed to connect to modbus device: %w", err)
	}
	defer func() {
		if err := handler.Close(); err != nil {
			logging.Error("failed to close modbus connection", "error", err)
		}
	}()

	var requestCounter model.UShort = 1
	result := &collectedRawValues{}

	// Dongle aggregate block (backlog 33): all live power values in ONE
	// request = one coherent snapshot. Grid power is sign-flipped to the
	// meter convention (positive = export) for the raw store and KPI types.
	logging.Info("reading dongle aggregate block", "device_id", devCfg.DongleDeviceId)
	dongleValues, err := readRegisters(handler, jobId, &requestCounter, devCfg.DongleDeviceId, registers.DongleBlockRegisters())
	if err != nil {
		return nil, err
	}
	applyDongleBlock(result, dongleValues)

	// Read inverter registers for each inverter device
	for _, deviceId := range devCfg.InverterDeviceIds {
		logging.Info("reading inverter registers", "device_id", deviceId)

		// Inverter daily yield (U32) - read separately; kept as raw reference
		// and as fallback while the lifetime-delta has no baseline (ADR-0014)
		dailyYield, err := readRegisters(handler, jobId, &requestCounter, deviceId, registers.InverterDailyYieldRegisters())
		if err != nil {
			return nil, err
		}
		result.InverterDailyYield += applyGain(dailyYield, registers.InverterDailyYield)

		// Inverter accumulated (lifetime) yield (U32) - read separately
		accumulatedYield, err := readRegisters(handler, jobId, &requestCounter, deviceId, registers.InverterAccumulatedYieldRegisters())
		if err != nil {
			return nil, err
		}
		result.InverterAccumulatedYield += applyGain(accumulatedYield, registers.InverterAccumulatedYield)
	}

	// Read meter registers for each meter device (energy counters only — the
	// live grid power comes from the dongle block above)
	for _, deviceId := range devCfg.MeterDeviceIds {
		logging.Info("reading meter registers", "device_id", deviceId)

		// Meter energy registers (consecutive U32 block: 37119 + 37121)
		meterEnergyValues, err := readRegisters(handler, jobId, &requestCounter, deviceId, registers.MeterEnergyRegisters())
		if err != nil {
			return nil, err
		}
		result.MeterExportedEnergy += applyGain(meterEnergyValues, registers.MeterExportedEnergy)
		result.MeterImportedEnergy += applyGain(meterEnergyValues, registers.MeterImportedEnergy)
	}

	return result, nil
}

// applyDongleBlock maps the dongle's aggregate block onto the collected raw
// values. Register 37516 lands in InverterActivePower (kW, via its THOUSAND
// gain); grid register 37502 (positive = import) is sign-flipped into
// MeterActivePower's meter convention (positive = export).
func applyDongleBlock(result *collectedRawValues, values []*response.RegisterValue) {
	result.InverterActivePower = applyGain(values, registers.DongleTotalActivePower)
	result.MeterActivePower = -applyGain(values, registers.DongleGridPower)
	result.LoadPower = applyGain(values, registers.DongleLoadPower)
	result.BatteryPower = applyGain(values, registers.DongleBatteryPower)
}

func readRegisters(handler *modbus.TcpHandler, jobId string, requestCounter *model.UShort, deviceId uint8, regDefs []model.RegisterDefinition) ([]*response.RegisterValue, error) {
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
		return nil, fmt.Errorf("failed to execute modbus request: %w", err)
	}

	resp := response.NewRegisterValueResponseFromBytes(regDefs, req.JobId, req.RequestId, req.RequestTime, *header, pduBytes)
	return resp.Values, nil
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

// computeLiveKpis maps the dongle block onto the live KPIs — values are taken
// from registers wherever one exists (Producing ← 37516, Consuming ← 37500,
// grid exchange ← 37502); only SelfConsuming has no register and stays derived.
func computeLiveKpis(raw *collectedRawValues) *kpis.KpiLiveData {
	inverterPower := kpis.NewInverterActivePower(raw.InverterActivePower, kpis.ParseUnitFromString("kW"))
	meterPower := kpis.NewMeterActivePower(raw.MeterActivePower, kpis.ParseUnitFromString("W"))
	consumingPower := kpis.NewConsumingPower(raw.LoadPower, kpis.ParseUnitFromString("W"))
	return &kpis.KpiLiveData{
		InverterActivePower: inverterPower,
		MeterActivePower:    meterPower,
		ConsumingPower:      consumingPower,
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

func computeTodayKpis(raw *collectedRawValues, energyToday *EnergyTodayResponse) *kpis.KpiTodayData {
	// Produced comes from the lifetime-counter delta (ADR-0014): immune to the
	// midnight reset of the device's daily register. Only while the data
	// accessor has no baseline yet does the daily register fill in.
	produced := raw.InverterDailyYield
	if energyToday.ProducedEnergyToday != nil {
		produced = *energyToday.ProducedEnergyToday
	}
	inverterEnergy := kpis.NewInverterEnergyToday(produced, kpis.ParseUnitFromString("kWh"))
	meterEnergyToday := kpis.NewMeterEnergyToday(
		energyToday.ImportedEnergyToday, kpis.ParseUnitFromString("kWh"),
		energyToday.ExportedEnergyToday, kpis.ParseUnitFromString("kWh"),
	)
	return &kpis.KpiTodayData{
		InverterTodayEnergy: inverterEnergy,
		MeterTodayEnergy:    meterEnergyToday,
	}
}
