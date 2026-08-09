package metrics

import (
	"math"
	"net"
	"net/http"
	"net/http/httptest"
	"pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"strconv"
	"sync/atomic"
	"testing"

	"github.com/prometheus/client_golang/prometheus"
	dto "github.com/prometheus/client_model/go"
)

func TestNewMetricsPushService_CreatesAllGauges(t *testing.T) {
	cfg := &config.PushGatewayConfig{
		Protocol: "http",
		Host:     "localhost",
		Port:     9091,
		Enabled:  true,
	}
	svc := NewMetricsPushService(cfg)

	if svc.pusher == nil {
		t.Fatal("pusher should be initialized")
	}
	if svc.producingWatts == nil {
		t.Error("producingWatts gauge should be initialized")
	}
	if svc.selfConsumedTodayWh == nil {
		t.Error("selfConsumedTodayWh gauge should be initialized")
	}
}

func TestPushKpiMetrics_SetsGaugeValuesAndPushes(t *testing.T) {
	var pushReceived atomic.Bool
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		pushReceived.Store(true)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	host, portStr, err := net.SplitHostPort(server.Listener.Addr().String())
	if err != nil {
		t.Fatalf("failed to parse test server address: %v", err)
	}
	port, err := strconv.Atoi(portStr)
	if err != nil {
		t.Fatalf("failed to parse port: %v", err)
	}

	svc := NewMetricsPushService(&config.PushGatewayConfig{
		Protocol: "http",
		Host:     host,
		Port:     port,
		Enabled:  true,
	})

	kpiValues := &kpis.PvLoggerKpiData{
		JobId:         "test-metrics",
		Producing:     kpis.W{W: 5000},
		Exporting:     kpis.W{W: 2000},
		Importing:     kpis.W{W: 0},
		Consuming:     kpis.W{W: 3000},
		SelfConsuming: kpis.W{W: 3000},
		Produced:      kpis.Wh{Wh: 15000},
		Exported:      kpis.Wh{Wh: 8000},
		Imported:      kpis.Wh{Wh: 1000},
		Consumed:      kpis.Wh{Wh: 8000},
		SelfConsumed:  kpis.Wh{Wh: 7000},
	}

	svc.PushKpiMetrics(kpiValues)

	assertGaugeValue(t, svc.producingWatts, 5000, "producingWatts")
	assertGaugeValue(t, svc.exportingWatts, 2000, "exportingWatts")
	assertGaugeValue(t, svc.importingWatts, 0, "importingWatts")
	assertGaugeValue(t, svc.consumingWatts, 3000, "consumingWatts")
	assertGaugeValue(t, svc.selfConsumingWatts, 3000, "selfConsumingWatts")
	assertGaugeValue(t, svc.producedTodayWh, 15000, "producedTodayWh")
	assertGaugeValue(t, svc.exportedTodayWh, 8000, "exportedTodayWh")
	assertGaugeValue(t, svc.importedTodayWh, 1000, "importedTodayWh")
	assertGaugeValue(t, svc.consumedTodayWh, 8000, "consumedTodayWh")
	assertGaugeValue(t, svc.selfConsumedTodayWh, 7000, "selfConsumedTodayWh")

	if !pushReceived.Load() {
		t.Error("metrics should have been pushed to server")
	}
}

func assertGaugeValue(t *testing.T, gauge prometheus.Gauge, want float64, name string) {
	t.Helper()
	m := &dto.Metric{}
	if err := gauge.Write(m); err != nil {
		t.Fatalf("failed to read gauge %s: %v", name, err)
	}
	got := m.GetGauge().GetValue()
	if math.Abs(got-want) > 0.001 {
		t.Errorf("gauge %s = %f, want %f", name, got, want)
	}
}
