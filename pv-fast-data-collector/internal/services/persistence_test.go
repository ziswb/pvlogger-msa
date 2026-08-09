package services

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"pvlogger-shared-modbus/kpis"
	"sync"
	"testing"
)

func TestPersistenceService_StoreRawValues(t *testing.T) {
	var (
		mu           sync.Mutex
		receivedBody RawKpiRegisterValues
		method       string
		contentType  string
	)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		defer mu.Unlock()
		method = r.Method
		contentType = r.Header.Get("Content-Type")
		_ = json.NewDecoder(r.Body).Decode(&receivedBody)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	svc := &PersistenceService{
		baseURL:    server.URL,
		httpClient: server.Client(),
	}

	t.Setenv("DA_BASE_PATH", "v1")
	t.Setenv("DA_STORE_RAW_KPI_REGISTER_VALUES_PATH", "store-raw-kpi-register-values")

	values := &RawKpiRegisterValues{
		JobId:               "test-job",
		InverterActivePower: 5.5,
		MeterActivePower:    -200.0,
		InverterDailyYield:  12.3,
		MeterExportedEnergy: 8.0,
		MeterImportedEnergy: 2.0,
	}
	if err := svc.StoreRawValues(values); err != nil {
		t.Fatalf("StoreRawValues returned error: %v", err)
	}

	mu.Lock()
	defer mu.Unlock()
	if method != http.MethodPost {
		t.Errorf("method = %s, want POST", method)
	}
	if contentType != "application/json" {
		t.Errorf("Content-Type = %s, want application/json", contentType)
	}
	if receivedBody.JobId != "test-job" {
		t.Errorf("JobId = %q, want %q", receivedBody.JobId, "test-job")
	}
	if receivedBody.InverterActivePower != 5.5 {
		t.Errorf("InverterActivePower = %f, want 5.5", receivedBody.InverterActivePower)
	}
	if receivedBody.MeterActivePower != -200.0 {
		t.Errorf("MeterActivePower = %f, want -200.0", receivedBody.MeterActivePower)
	}
}

func TestPersistenceService_StoreKpiValues(t *testing.T) {
	var (
		mu           sync.Mutex
		receivedBody kpis.PvLoggerKpiData
	)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		defer mu.Unlock()
		_ = json.NewDecoder(r.Body).Decode(&receivedBody)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	svc := &PersistenceService{
		baseURL:    server.URL,
		httpClient: server.Client(),
	}

	t.Setenv("DA_BASE_PATH", "v1")
	t.Setenv("DA_STORE_KPI_VALUES_PATH", "store-kpi-values")

	kpiValues := &kpis.PvLoggerKpiData{
		JobId:     "kpi-job",
		Producing: kpis.W{W: 5000},
		Exporting: kpis.W{W: 2000},
		Importing: kpis.W{W: 0},
		Produced:  kpis.Wh{Wh: 15000},
		Exported:  kpis.Wh{Wh: 8000},
	}
	if err := svc.StoreKpiValues(kpiValues); err != nil {
		t.Fatalf("StoreKpiValues returned error: %v", err)
	}

	mu.Lock()
	defer mu.Unlock()
	if receivedBody.JobId != "kpi-job" {
		t.Errorf("JobId = %q, want %q", receivedBody.JobId, "kpi-job")
	}
	if receivedBody.Producing.W != 5000 {
		t.Errorf("Producing = %f, want 5000", receivedBody.Producing.W)
	}
}

func TestPersistenceService_ReadMeterEnergyToday(t *testing.T) {
	var (
		mu     sync.Mutex
		method string
	)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		method = r.Method
		mu.Unlock()
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(MeterEnergyTodayResponse{
			ExportedEnergyToday: 10.5,
			ImportedEnergyToday: 3.2,
		})
	}))
	defer server.Close()

	svc := &PersistenceService{
		baseURL:    server.URL,
		httpClient: server.Client(),
	}

	t.Setenv("DA_BASE_PATH", "v1")
	t.Setenv("DA_READ_METER_ENERGY_TODAY_PATH", "meter-energy-today")

	result, err := svc.ReadMeterEnergyToday()
	if err != nil {
		t.Fatalf("ReadMeterEnergyToday returned error: %v", err)
	}

	mu.Lock()
	defer mu.Unlock()
	if method != http.MethodGet {
		t.Errorf("method = %s, want GET", method)
	}
	if result.ExportedEnergyToday != 10.5 {
		t.Errorf("ExportedEnergyToday = %f, want 10.5", result.ExportedEnergyToday)
	}
	if result.ImportedEnergyToday != 3.2 {
		t.Errorf("ImportedEnergyToday = %f, want 3.2", result.ImportedEnergyToday)
	}
}
