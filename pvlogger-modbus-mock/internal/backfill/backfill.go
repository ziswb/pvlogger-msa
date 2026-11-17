// Package backfill writes the synthetic plant's past as history (ADR-0025):
// for every minute the raw KPI row the fast collector would have stored and
// the KPI row the shared aggregation would have computed, dated that minute.
package backfill

import (
	"context"
	"fmt"
	"slices"
	"time"

	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/persistence"

	"pvlogger-modbus-mock/internal/mock"
	"pvlogger-modbus-mock/internal/plant"
)

// dayRow is the part of a /v1/daydata row the backfill needs.
type dayRow struct {
	CreatedAt int64 `json:"createdAt"`
}

// Run stores a raw KPI row and a KPI row for every minute in [from, until)
// that has no KPI row yet (live data or an earlier run), local day by local
// day. A rerun therefore completes a run that died mid-day and stores nothing
// once the history is complete.
//
// The day before until's day goes first: until its last raw row exists, the
// live data accessor has no midnight baseline and the live KPI rows it stores
// meanwhile record no exported or imported energy for today.
//
// Run stops between two minutes once ctx ends (SIGTERM on pod deletion); a
// rerun completes the history.
func Run(ctx context.Context, p *plant.Plant, client *persistence.PersistingService, from, until time.Time) error {
	loc := p.Location()
	start := from.In(loc).Truncate(time.Minute)
	for _, day := range dayOrder(plant.StartOfDay(start), until) {
		end := plant.NextDay(day)
		if until.Before(end) {
			end = until
		}
		begin := day
		if begin.Before(start) {
			begin = start
		}
		have, err := minutesWithRows(client, day)
		if err != nil {
			return err
		}
		stored, err := fill(ctx, p, client, begin, end, have)
		if err != nil {
			return err
		}
		if stored > 0 {
			logging.Info("backfilled", "day", day.Format(time.DateOnly), "minutes", stored)
		}
	}
	return nil
}

// dayOrder lists the starts of the local days in [first, until), the second
// newest first, then the newest, then the rest newest to oldest.
func dayOrder(first, until time.Time) []time.Time {
	var days []time.Time
	for day := first; day.Before(until); day = plant.NextDay(day) {
		days = append(days, day)
	}
	slices.Reverse(days)
	if len(days) > 1 {
		days[0], days[1] = days[1], days[0]
	}
	return days
}

// minutesWithRows returns the minutes of the day that already have a KPI row.
func minutesWithRows(client *persistence.PersistingService, day time.Time) (map[int64]bool, error) {
	var rows []dayRow
	path := fmt.Sprintf("%s/%d-%d-%d", sharedconfig.ReadDayDataPath, day.Year(), day.Month(), day.Day())
	if err := client.GetJSON(path, &rows); err != nil {
		return nil, fmt.Errorf("reading the KPI rows of %s: %w", day.Format(time.DateOnly), err)
	}
	have := make(map[int64]bool, len(rows))
	for _, r := range rows {
		have[time.UnixMilli(r.CreatedAt).Truncate(time.Minute).UnixMilli()] = true
	}
	return have, nil
}

// fill stores the rows of every minute in [begin, end) not in have and
// returns how many minutes it stored. A refused raw row (400) usually means
// the data accessor is not a dev build (DEV_ACCEPT_CLIENT_TIMESTAMPS, ADR-0025).
func fill(ctx context.Context, p *plant.Plant, client *persistence.PersistingService, begin, end time.Time, have map[int64]bool) (int, error) {
	stored := 0
	for at := begin; at.Before(end); at = at.Add(time.Minute) {
		if err := ctx.Err(); err != nil {
			return stored, err
		}
		if have[at.UnixMilli()] {
			continue
		}
		r := mock.ReadingOf(p.At(at))
		midnight := mock.ReadingOf(p.At(plant.StartOfDay(at)))
		jobId := "backfill-" + at.UTC().Format("20060102T1504")
		if err := client.StoreValues(rawRowOf(jobId, at, r), sharedconfig.StoreRawKpiRegisterValuesPath); err != nil {
			return stored, fmt.Errorf("storing the raw row of %s (is the data accessor a dev build, DEV_ACCEPT_CLIENT_TIMESTAMPS=true?): %w", at.Format(time.DateTime), err)
		}
		if err := client.StoreValues(kpiRowOf(jobId, at, r, midnight), sharedconfig.StoreKpiValuesPath); err != nil {
			return stored, fmt.Errorf("storing the KPI row of %s: %w", at.Format(time.DateTime), err)
		}
		stored++
	}
	return stored, nil
}

func rawRowOf(jobId string, at time.Time, r mock.Reading) *persistence.RawKpiRegisterValues {
	ms := at.UnixMilli()
	return &persistence.RawKpiRegisterValues{
		JobId:                    jobId,
		InverterActivePower:      r.PVW / 1000,
		MeterActivePower:         -r.GridImportW,
		InverterDailyYield:       r.YieldTodayKWh,
		InverterAccumulatedYield: r.YieldTotalKWh,
		MeterExportedEnergy:      r.ExportedTotalKWh,
		MeterImportedEnergy:      r.ImportedTotalKWh,
		// Accepted only by a dev build of the data accessor (ADR-0025).
		DateCreated: &ms,
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
