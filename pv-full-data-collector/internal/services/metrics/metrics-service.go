package metrics

import (
	"pv-full-data-collector/internal/kpis"
	"pv-logger/logging"
)

func (s *MetricsPushService) PushKpiMetrics(kpiValues *kpis.PvLoggerKpiData) {
	s.producingWatts.Set(kpiValues.Producing.W)
	s.exportingWatts.Set(kpiValues.Exporting.W)
	s.importingWatts.Set(kpiValues.Importing.W)
	s.consumingWatts.Set(kpiValues.Consuming.W)
	s.selfConsumingWatts.Set(kpiValues.SelfConsuming.W)
	s.producedTodayWh.Set(kpiValues.Produced.Wh)
	s.exportedTodayWh.Set(kpiValues.Exported.Wh)
	s.importedTodayWh.Set(kpiValues.Imported.Wh)
	s.consumedTodayWh.Set(kpiValues.Consumed.Wh)
	s.selfConsumedTodayWh.Set(kpiValues.SelfConsumed.Wh)

	if err := s.pusher.Push(); err != nil {
		logging.Error("failed to push metrics to push gateway", "error", err)
	} else {
		logging.Info("metrics pushed to push gateway")
	}
}
