// Package backfill writes the synthetic plant's past as history (ADR-0025):
// for every minute the raw KPI row the fast collector would have stored and
// the KPI row the shared aggregation would have computed, dated that minute.
package backfill

import (
	"fmt"
	"time"

	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/persistence"

	"pvlogger-modbus-mock/internal/mock"
	"pvlogger-modbus-mock/internal/plant"
)

// rawKpiRegisterValues is the data accessor's raw-row DTO; dateCreated is
// honoured only with DEV_ACCEPT_CLIENT_TIMESTAMPS.
type rawKpiRegisterValues struct {
	JobId                    string  `json:"jobId"`
	InverterActivePower      float64 `json:"inverterActivePower"` // kW
	MeterActivePower         float64 `json:"meterActivePower"`    // W, positive = export
	InverterDailyYield       float64 `json:"inverterDailyYield"`
	InverterAccumulatedYield float64 `json:"inverterAccumulatedYield"`
	MeterExportedEnergy      float64 `json:"meterExportedEnergy"`
	MeterImportedEnergy      float64 `json:"meterImportedEnergy"`
	DateCreated              int64   `json:"dateCreated"`
}

// dayRow is the part of a /v1/daydata row the backfill needs.
type dayRow struct {
	CreatedAt int64 `json:"createdAt"`
}

// Run stores a raw KPI row and a KPI row for every minute in [from, until),
// local day by local day. A day that already has KPI rows (live data or an
// earlier run) is filled only up to its first row, so a rerun stores nothing
// and the history ends where the live data begins.
func Run(p *plant.Plant, client *persistence.PersistingService, from, until time.Time) error {
	loc := p.Location()
	start := from.In(loc).Truncate(time.Minute)
	for day := localMidnight(start); day.Before(until); day = day.AddDate(0, 0, 1) {
		end := day.AddDate(0, 0, 1)
		if until.Before(end) {
			end = until
		}
		first, err := firstRowOf(client, day)
		if err != nil {
			return err
		}
		if first != nil && first.Before(end) {
			end = *first
		}
		begin := day
		if begin.Before(start) {
			begin = start
		}
		if err := fill(p, client, begin, end); err != nil {
			return err
		}
		if begin.Before(end) {
			logging.Info("backfilled", "day", day.Format(time.DateOnly), "from", begin.Format(time.TimeOnly), "until", end.Format(time.TimeOnly))
		}
	}
	return nil
}

func localMidnight(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, t.Location())
}

// firstRowOf returns when the day's first KPI row was stored, or nil.
func firstRowOf(client *persistence.PersistingService, day time.Time) (*time.Time, error) {
	var rows []dayRow
	path := fmt.Sprintf("%s/%d-%d-%d", sharedconfig.ReadDayDataPath, day.Year(), day.Month(), day.Day())
	if err := client.GetJSON(path, &rows); err != nil {
		return nil, fmt.Errorf("reading the KPI rows of %s: %w", day.Format(time.DateOnly), err)
	}
	var first *time.Time
	for _, r := range rows {
		if at := time.UnixMilli(r.CreatedAt); first == nil || at.Before(*first) {
			first = &at
		}
	}
	return first, nil
}

// fill stores the rows of every minute in [begin, end).
func fill(p *plant.Plant, client *persistence.PersistingService, begin, end time.Time) error {
	loc := p.Location()
	for at := begin; at.Before(end); at = at.Add(time.Minute) {
		r := mock.ReadingOf(p.At(at))
		midnight := mock.ReadingOf(p.At(time.Date(at.Year(), at.Month(), at.Day(), 0, 0, 0, 0, loc)))
		jobId := "backfill-" + at.UTC().Format("20060102T1504")
		if err := client.StoreValues(rawRowOf(jobId, at, r), sharedconfig.StoreRawKpiRegisterValuesPath); err != nil {
			return fmt.Errorf("storing the raw row of %s: %w", at.Format(time.DateTime), err)
		}
		if err := client.StoreValues(kpiRowOf(jobId, at, r, midnight), sharedconfig.StoreKpiValuesPath); err != nil {
			return fmt.Errorf("storing the KPI row of %s: %w", at.Format(time.DateTime), err)
		}
	}
	return nil
}

func rawRowOf(jobId string, at time.Time, r mock.Reading) *rawKpiRegisterValues {
	return &rawKpiRegisterValues{
		JobId:                    jobId,
		InverterActivePower:      r.PVW / 1000,
		MeterActivePower:         -r.GridImportW,
		InverterDailyYield:       r.YieldTodayKWh,
		InverterAccumulatedYield: r.YieldTotalKWh,
		MeterExportedEnergy:      r.ExportedTotalKWh,
		MeterImportedEnergy:      r.ImportedTotalKWh,
		DateCreated:              at.UnixMilli(),
	}
}

// kpiRowOf computes the KPIs with the shared kpis code, as the aggregation does:
// live power from the reading, today's energy as the counters' difference to
// local midnight.
func kpiRowOf(jobId string, at time.Time, r, midnight mock.Reading) *kpis.PvLoggerKpiData {
	w := kpis.ParseUnitFromString("W")
	kWh := kpis.ParseUnitFromString("kWh")
	live := kpis.KpiLiveData{
		InverterActivePower: kpis.NewInverterActivePower(r.PVW, w),
		MeterActivePower:    kpis.NewMeterActivePower(-r.GridImportW, w),
		ConsumingPower:      kpis.NewConsumingPower(r.LoadW, w),
	}
	today := kpis.KpiTodayData{
		InverterTodayEnergy: kpis.NewInverterEnergyToday(r.YieldTotalKWh-midnight.YieldTotalKWh, kWh),
		MeterTodayEnergy: kpis.NewMeterEnergyToday(
			r.ImportedTotalKWh-midnight.ImportedTotalKWh, kWh,
			r.ExportedTotalKWh-midnight.ExportedTotalKWh, kWh),
	}
	return &kpis.PvLoggerKpiData{
		JobId:         jobId,
		DateCreated:   at.UnixMilli(),
		Producing:     live.Producing(),
		Exporting:     live.Exporting(),
		Importing:     live.Importing(),
		Consuming:     live.Consuming(),
		SelfConsuming: live.SelfConsuming(),
		Produced:      today.Produced().ToWh(),
		Exported:      today.Exported().ToWh(),
		Imported:      today.Imported().ToWh(),
		Consumed:      today.Consumed().ToWh(),
		SelfConsumed:  today.SelfConsumed().ToWh(),
	}
}
