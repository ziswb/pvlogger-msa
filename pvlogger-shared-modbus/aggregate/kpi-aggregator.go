// Package aggregate is the single KPI aggregation path (ADR-0016): it reads a
// collection run's raw rows back from the data accessor and derives + stores
// the KPIs. Both collectors invoke it in-process after POSTing their raw
// values — the path is identical, not merely shared.
package aggregate

import (
	"errors"
	"fmt"
	"strconv"
	"time"

	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/metrics"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/persistence"
	"pvlogger-shared-modbus/registers"
)

// EnergyToday mirrors the data accessor's ADR-0014 day-delta response
// (meter-energy-today): today's energy figures derived from the lifetime
// counters of the raw KPI rows.
type EnergyToday struct {
	ExportedEnergyToday float64 `json:"exportedEnergyToday"`
	ImportedEnergyToday float64 `json:"importedEnergyToday"`
	// nil only while the accessor's raw-value store is completely empty — a
	// missing lifetime baseline is covered server-side by the daily-register
	// fallback inside the endpoint (ADR-0014).
	ProducedEnergyToday *float64 `json:"producedEnergyToday"`
}

// KpiAggregator derives the KPI row for one collection run from the values
// that run stored in the data accessor, and stores the result.
type KpiAggregator struct {
	client         *persistence.PersistingService
	dongleDeviceId model.UByte
}

func NewKpiAggregator() *KpiAggregator {
	daCfg := sharedconfig.DataAccessorCfg()
	return newKpiAggregator(
		persistence.NewPersistingService(daCfg.Host, daCfg.Port, daCfg.Protocol, sharedconfig.DataAccessorBasePath),
		sharedconfig.DevIdConfig().DongleDeviceId,
	)
}

func newKpiAggregator(client *persistence.PersistingService, dongleDeviceId model.UByte) *KpiAggregator {
	return &KpiAggregator{client: client, dongleDeviceId: dongleDeviceId}
}

// KpisFor computes and stores the KPI row for one collection run. Hard
// invariant (ADR-0016): every register read is per jobId — one run, one
// coherent snapshot; never "newest value per register" across runs, which
// would mix interleaved fast/full runs and reintroduce the sampling skew
// backlog 33 removed.
func (a *KpiAggregator) KpisFor(jobId model.JobId) error {
	logging.Info("aggregating KPIs", "job_id", jobId.Id)
	liveKpis, err := a.liveKpis(jobId)
	if err != nil {
		return err
	}
	todayKpis, err := a.todayKpis()
	if err != nil {
		return err
	}
	kpiValues := kpis.PvLoggerKpiData{
		JobId:         jobId.Id,
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
	if err := a.client.StoreValues(&kpiValues, sharedconfig.StoreKpiValuesPath); err != nil {
		return err
	}
	pushKpiMetrics(&kpiValues)
	logging.Info("KPIs computed and stored", "job_id", jobId.Id)
	return nil
}

// liveKpis maps the dongle's unit-100 aggregate block onto the live KPIs
// (backlog 33): Producing ← 37516, Consuming ← 37500 (measured directly),
// grid exchange ← 37502 sign-flipped to the meter convention (positive =
// export). Only SelfConsuming has no register and stays derived.
func (a *KpiAggregator) liveKpis(jobId model.JobId) (*kpis.KpiLiveData, error) {
	totalActivePower, err := a.registerValue(registers.DongleTotalActivePower, jobId)
	if err != nil {
		return nil, err
	}
	loadPower, err := a.registerValue(registers.DongleLoadPower, jobId)
	if err != nil {
		return nil, err
	}
	gridPower, err := a.registerValue(registers.DongleGridPower, jobId)
	if err != nil {
		return nil, err
	}
	return &kpis.KpiLiveData{
		InverterActivePower: kpis.NewInverterActivePower(totalActivePower, kpis.ParseUnitFromString(registers.DongleTotalActivePower.Unit)),
		MeterActivePower:    kpis.NewMeterActivePower(-gridPower, kpis.ParseUnitFromString(registers.DongleGridPower.Unit)),
		ConsumingPower:      kpis.NewConsumingPower(loadPower, kpis.ParseUnitFromString(registers.DongleLoadPower.Unit)),
	}, nil
}

// todayKpis fetches today's energy figures from the accessor's ADR-0014
// day-delta endpoint. The deltas are anchored on the newest raw KPI row, so
// the caller must have stored its raw values before aggregating.
func (a *KpiAggregator) todayKpis() (*kpis.KpiTodayData, error) {
	var energy EnergyToday
	if err := a.client.GetJSON(sharedconfig.ReadMeterEnergyTodayPath, &energy); err != nil {
		return nil, fmt.Errorf("error reading energy today: %w", err)
	}
	logging.Info("energy today",
		"exported", energy.ExportedEnergyToday,
		"imported", energy.ImportedEnergyToday,
		"produced", energy.ProducedEnergyToday,
	)
	produced := 0.0
	if energy.ProducedEnergyToday != nil {
		produced = *energy.ProducedEnergyToday
	} else {
		logging.Warn("no produced-energy figure available (empty raw-value store?), assuming 0")
	}
	kwhUnit := kpis.ParseUnitFromString("kWh")
	return &kpis.KpiTodayData{
		InverterTodayEnergy: kpis.NewInverterEnergyToday(produced, kwhUnit),
		MeterTodayEnergy:    kpis.NewMeterEnergyToday(energy.ImportedEnergyToday, kwhUnit, energy.ExportedEnergyToday, kwhUnit),
	}, nil
}

// registerValue reads the raw value this run stored for one dongle register
// and applies the definition's gain.
func (a *KpiAggregator) registerValue(def model.RegisterDefinition, jobId model.JobId) (float64, error) {
	path := fmt.Sprintf("%s/%s/%d/%d/%s",
		sharedconfig.ReadLatestRegisterValuePath, def.Type, a.dongleDeviceId, def.RegisterAddress.Value, jobId.Id)
	var raw float64
	if err := a.client.GetJSON(path, &raw); err != nil {
		// Only a 404 means the row is genuinely absent, which is what the
		// ordering invariant below is about. A timeout or a refused connection
		// tells us nothing about the store's contents, so claiming an ordering
		// violation there sends the reader hunting for the wrong bug — see
		// backlog 38.
		if errors.Is(err, persistence.ErrNotFound) {
			return 0, fmt.Errorf(
				"no value for register %d (%s) in run %s — the run must store the dongle aggregate block before aggregating (full collector: backlog 35): %w",
				def.RegisterAddress.Value, def.Name, jobId.Id, err)
		}
		return 0, fmt.Errorf(
			"could not read register %d (%s) for run %s from the data accessor: %w",
			def.RegisterAddress.Value, def.Name, jobId.Id, err)
	}
	result := raw / gainValue(def)
	if logging.DebugEnabled() {
		logging.Debug("gain applied", "register", def.Name, "raw", raw, "gain", def.Gain, "result", result)
	}
	return result, nil
}

func gainValue(def model.RegisterDefinition) float64 {
	value, err := strconv.ParseFloat(kpis.NewGainFromString(def.Gain).String(), 64)
	if err != nil {
		return 1.0
	}
	return value
}

func pushKpiMetrics(kpiValues *kpis.PvLoggerKpiData) {
	cfg := sharedconfig.PushGatewayCfg()
	if !cfg.Enabled {
		logging.Debug("push gateway is disabled")
		return
	}
	metrics.NewMetricsPushService(cfg).PushKpiMetrics(kpiValues)
}
