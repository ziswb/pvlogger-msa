package aggregate

import (
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/kpis"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/persistence"
	"pvlogger-shared-modbus/registers"
	"strconv"
	"strings"
	"sync"
	"testing"
)

const testDongleDeviceId = model.UByte(100)

// fakeAccessor is an httptest data accessor serving per-jobId register values
// and the day-delta endpoint, and capturing the stored KPI row.
type fakeAccessor struct {
	t *testing.T
	// registerValues maps "type/deviceId/address/jobId" to the raw value body.
	registerValues map[string]string
	energyToday    string

	mu        sync.Mutex
	storedKpi *kpis.PvLoggerKpiData
}

func (f *fakeAccessor) handler() http.Handler {
	mux := http.NewServeMux()
	prefix := "/" + sharedconfig.DataAccessorBasePath
	mux.HandleFunc(prefix+"/"+sharedconfig.ReadLatestRegisterValuePath+"/", func(w http.ResponseWriter, r *http.Request) {
		key := r.URL.Path[len(prefix+"/"+sharedconfig.ReadLatestRegisterValuePath+"/"):]
		body, ok := f.registerValues[key]
		if !ok {
			w.WriteHeader(http.StatusNotFound)
			return
		}
		_, _ = w.Write([]byte(body))
	})
	mux.HandleFunc(prefix+"/"+sharedconfig.ReadMeterEnergyTodayPath, func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(f.energyToday))
	})
	mux.HandleFunc(prefix+"/"+sharedconfig.StoreKpiValuesPath, func(w http.ResponseWriter, r *http.Request) {
		f.mu.Lock()
		defer f.mu.Unlock()
		var kpi kpis.PvLoggerKpiData
		if err := json.NewDecoder(r.Body).Decode(&kpi); err != nil {
			f.t.Errorf("failed to decode stored KPI row: %v", err)
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		f.storedKpi = &kpi
		w.WriteHeader(http.StatusOK)
	})
	return mux
}

func registerKey(def model.RegisterDefinition, jobId string) string {
	return fmt.Sprintf("%s/%d/%d/%s", def.Type, testDongleDeviceId, def.RegisterAddress.Value, jobId)
}

func testAggregator(t *testing.T, server *httptest.Server) *KpiAggregator {
	t.Helper()
	u, err := url.Parse(server.URL)
	if err != nil {
		t.Fatalf("failed to parse test server URL: %v", err)
	}
	host, portStr, err := net.SplitHostPort(u.Host)
	if err != nil {
		t.Fatalf("failed to split test server host/port: %v", err)
	}
	port, err := strconv.Atoi(portStr)
	if err != nil {
		t.Fatalf("failed to parse test server port: %v", err)
	}
	return newKpiAggregator(
		persistence.NewPersistingService(host, port, "http", sharedconfig.DataAccessorBasePath),
		testDongleDeviceId,
	)
}

// TestKpisFor_DaytimeExport mirrors the live-verified daytime sample (backlog
// 33): total_active 6974 W, load 885 W, grid -6089 W (dongle convention:
// negative = export). The whole live mapping is asserted, including the sign
// flip and that total_active + grid = load stays consistent.
func TestKpisFor_DaytimeExport(t *testing.T) {
	fake := &fakeAccessor{
		t: t,
		registerValues: map[string]string{
			registerKey(registers.DongleTotalActivePower, "job-day"): "6974",
			registerKey(registers.DongleLoadPower, "job-day"):        "885",
			registerKey(registers.DongleGridPower, "job-day"):        "-6089",
		},
		energyToday: `{"exportedEnergyToday": 8.0, "importedEnergyToday": 2.0, "producedEnergyToday": 14.0}`,
	}
	server := httptest.NewServer(fake.handler())
	defer server.Close()

	if err := testAggregator(t, server).KpisFor(model.NewJobId("job-day")); err != nil {
		t.Fatalf("KpisFor returned error: %v", err)
	}

	fake.mu.Lock()
	defer fake.mu.Unlock()
	if fake.storedKpi == nil {
		t.Fatal("no KPI row was stored")
	}
	if fake.storedKpi.JobId != "job-day" {
		t.Errorf("JobId = %q, want job-day", fake.storedKpi.JobId)
	}
	assertFloat(t, "Producing", fake.storedKpi.Producing.W, 6974)
	assertFloat(t, "Exporting", fake.storedKpi.Exporting.W, 6089)
	assertFloat(t, "Importing", fake.storedKpi.Importing.W, 0)
	assertFloat(t, "Consuming", fake.storedKpi.Consuming.W, 885)
	assertFloat(t, "SelfConsuming", fake.storedKpi.SelfConsuming.W, 885)
	assertFloat(t, "Produced", fake.storedKpi.Produced.Wh, 14000)
	assertFloat(t, "Exported", fake.storedKpi.Exported.Wh, 8000)
	assertFloat(t, "Imported", fake.storedKpi.Imported.Wh, 2000)
	assertFloat(t, "Consumed", fake.storedKpi.Consumed.Wh, 8000)
	assertFloat(t, "SelfConsumed", fake.storedKpi.SelfConsumed.Wh, 6000)
}

// TestKpisFor_NightImport mirrors the live-verified night sample: production
// 0, load 624 W fully imported (grid +624, dongle convention: positive =
// import).
func TestKpisFor_NightImport(t *testing.T) {
	fake := &fakeAccessor{
		t: t,
		registerValues: map[string]string{
			registerKey(registers.DongleTotalActivePower, "job-night"): "0",
			registerKey(registers.DongleLoadPower, "job-night"):        "624",
			registerKey(registers.DongleGridPower, "job-night"):        "624",
		},
		energyToday: `{"exportedEnergyToday": 8.0, "importedEnergyToday": 2.5, "producedEnergyToday": 14.0}`,
	}
	server := httptest.NewServer(fake.handler())
	defer server.Close()

	if err := testAggregator(t, server).KpisFor(model.NewJobId("job-night")); err != nil {
		t.Fatalf("KpisFor returned error: %v", err)
	}

	fake.mu.Lock()
	defer fake.mu.Unlock()
	assertFloat(t, "Producing", fake.storedKpi.Producing.W, 0)
	assertFloat(t, "Exporting", fake.storedKpi.Exporting.W, 0)
	assertFloat(t, "Importing", fake.storedKpi.Importing.W, 624)
	assertFloat(t, "Consuming", fake.storedKpi.Consuming.W, 624)
	assertFloat(t, "SelfConsuming", fake.storedKpi.SelfConsuming.W, 0)
}

func TestKpisFor_ProducedNullMeansEmptyStore(t *testing.T) {
	// producedEnergyToday is null only while the accessor's raw store is
	// completely empty (the daily-register fallback lives server-side since
	// ADR-0016) — the aggregation stores 0 rather than failing the run.
	fake := &fakeAccessor{
		t: t,
		registerValues: map[string]string{
			registerKey(registers.DongleTotalActivePower, "job-empty"): "1000",
			registerKey(registers.DongleLoadPower, "job-empty"):        "500",
			registerKey(registers.DongleGridPower, "job-empty"):        "-500",
		},
		energyToday: `{"exportedEnergyToday": 0.0, "importedEnergyToday": 0.0, "producedEnergyToday": null}`,
	}
	server := httptest.NewServer(fake.handler())
	defer server.Close()

	if err := testAggregator(t, server).KpisFor(model.NewJobId("job-empty")); err != nil {
		t.Fatalf("KpisFor returned error: %v", err)
	}

	fake.mu.Lock()
	defer fake.mu.Unlock()
	assertFloat(t, "Produced", fake.storedKpi.Produced.Wh, 0)
}

func TestKpisFor_FailsWithoutDongleBlockInRun(t *testing.T) {
	// A run that never stored the dongle aggregate block (e.g. the full
	// collector before masterdata knows the DONGLE device type, backlog 35)
	// must fail aggregation — never fall back to values from other runs
	// (ADR-0016 hard invariant: per-jobId only).
	fake := &fakeAccessor{
		t:              t,
		registerValues: map[string]string{},
		energyToday:    `{"exportedEnergyToday": 8.0, "importedEnergyToday": 2.0, "producedEnergyToday": 14.0}`,
	}
	server := httptest.NewServer(fake.handler())
	defer server.Close()

	err := testAggregator(t, server).KpisFor(model.NewJobId("job-missing"))
	if err == nil {
		t.Fatal("expected error for a run without the dongle block, got nil")
	}
	// A genuine 404 is the one case that IS an ordering violation, so the hint
	// belongs here. Paired with TestKpisFor_TransportFailureIsNotBlamedOnOrdering
	// below, which pins the opposite case (backlog 38).
	if !strings.Contains(err.Error(), "must store the dongle aggregate block") {
		t.Errorf("expected the ordering hint for a 404, got: %v", err)
	}
	if !errors.Is(err, persistence.ErrNotFound) {
		t.Errorf("expected the error to wrap persistence.ErrNotFound, got: %v", err)
	}
	fake.mu.Lock()
	defer fake.mu.Unlock()
	if fake.storedKpi != nil {
		t.Error("a KPI row was stored despite the missing dongle block")
	}
}

func TestKpisFor_TransportFailureIsNotBlamedOnOrdering(t *testing.T) {
	// Regression test for backlog 38. When the accessor never answers, the
	// error must not claim the run failed to store the dongle aggregate block:
	// that message sent twelve days of investigation at the collector while the
	// real fault was a 29.6 s query in PostgreSQL. A transport failure says
	// nothing about what the store contains.
	fake := &fakeAccessor{
		t:              t,
		registerValues: map[string]string{},
		energyToday:    `{"exportedEnergyToday": 8.0, "importedEnergyToday": 2.0, "producedEnergyToday": 14.0}`,
	}
	server := httptest.NewServer(fake.handler())
	// Closing the server before the call makes every request fail at the
	// transport layer, the same error class as the client timeout in the
	// incident.
	aggregator := testAggregator(t, server)
	server.Close()

	err := aggregator.KpisFor(model.NewJobId("job-unreachable"))
	if err == nil {
		t.Fatal("expected error when the accessor is unreachable, got nil")
	}
	if strings.Contains(err.Error(), "must store the dongle aggregate block") {
		t.Errorf("transport failure was reported as an ordering violation: %v", err)
	}
	if errors.Is(err, persistence.ErrNotFound) {
		t.Errorf("transport failure must not wrap ErrNotFound: %v", err)
	}
	if !strings.Contains(err.Error(), "could not read register") {
		t.Errorf("expected a transport-flavoured message, got: %v", err)
	}
}

func assertFloat(t *testing.T, name string, got, want float64) {
	t.Helper()
	if math.Abs(got-want) > 0.001 {
		t.Errorf("%s = %f, want %f", name, got, want)
	}
}
