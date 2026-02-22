package metrics

import (
	"fmt"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/push"
	"pv-quick-data-collector/internal/config"
)

type MetricsPushService struct {
	pusher              *push.Pusher
	producingWatts      prometheus.Gauge
	exportingWatts      prometheus.Gauge
	importingWatts      prometheus.Gauge
	consumingWatts      prometheus.Gauge
	selfConsumingWatts  prometheus.Gauge
	producedTodayWh     prometheus.Gauge
	exportedTodayWh     prometheus.Gauge
	importedTodayWh     prometheus.Gauge
	consumedTodayWh     prometheus.Gauge
	selfConsumedTodayWh prometheus.Gauge
}

func NewMetricsPushService(cfg *config.PushGatewayConfig) *MetricsPushService {
	registry := prometheus.NewRegistry()

	producingWatts := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "producing_watts",
		Help:      "Current power production in watts",
	})
	exportingWatts := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "exporting_watts",
		Help:      "Current power export to grid in watts",
	})
	importingWatts := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "importing_watts",
		Help:      "Current power import from grid in watts",
	})
	consumingWatts := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "consuming_watts",
		Help:      "Current power consumption in watts",
	})
	selfConsumingWatts := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "self_consuming_watts",
		Help:      "Current self-consumed power in watts",
	})
	producedTodayWh := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "produced_today_wh",
		Help:      "Energy produced today so far in watt-hours",
	})
	exportedTodayWh := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "exported_today_wh",
		Help:      "Energy exported today so far in watt-hours",
	})
	importedTodayWh := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "imported_today_wh",
		Help:      "Energy imported today so far in watt-hours",
	})
	consumedTodayWh := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "consumed_today_wh",
		Help:      "Energy consumed today so far in watt-hours",
	})
	selfConsumedTodayWh := prometheus.NewGauge(prometheus.GaugeOpts{
		Namespace: "pvlogger",
		Subsystem: "solar",
		Name:      "self_consumed_today_wh",
		Help:      "Energy self-consumed today so far in watt-hours",
	})

	registry.MustRegister(
		producingWatts,
		exportingWatts,
		importingWatts,
		consumingWatts,
		selfConsumingWatts,
		producedTodayWh,
		exportedTodayWh,
		importedTodayWh,
		consumedTodayWh,
		selfConsumedTodayWh,
	)

	pushGatewayURL := fmt.Sprintf("%s://%s:%d", cfg.Protocol, cfg.Host, cfg.Port)
	pusher := push.New(pushGatewayURL, "pvlogger_modbus_data_collector").Gatherer(registry)

	return &MetricsPushService{
		pusher:              pusher,
		producingWatts:      producingWatts,
		exportingWatts:      exportingWatts,
		importingWatts:      importingWatts,
		consumingWatts:      consumingWatts,
		selfConsumingWatts:  selfConsumingWatts,
		producedTodayWh:     producedTodayWh,
		exportedTodayWh:     exportedTodayWh,
		importedTodayWh:     importedTodayWh,
		consumedTodayWh:     consumedTodayWh,
		selfConsumedTodayWh: selfConsumedTodayWh,
	}
}
