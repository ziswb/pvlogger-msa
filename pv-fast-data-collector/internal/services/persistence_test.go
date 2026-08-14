package services

import (
	"encoding/json"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/persistence"
	"pvlogger-shared-modbus/registers"
	"pvlogger-shared-modbus/response"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"
)

// testService wires a PersistenceService to an httptest server through the
// shared persistence client.
func testService(t *testing.T, server *httptest.Server) *PersistenceService {
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
	return &PersistenceService{client: persistence.NewPersistingService(host, port, "http", sharedconfig.DataAccessorBasePath)}
}

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

	svc := testService(t, server)

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

func TestPersistenceService_StoreModbusValues(t *testing.T) {
	var (
		mu           sync.Mutex
		requestPath  string
		receivedBody map[string]any
	)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		defer mu.Unlock()
		requestPath = r.URL.Path
		_ = json.NewDecoder(r.Body).Decode(&receivedBody)
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	svc := testService(t, server)

	resp := &response.RegisterValueResponse{
		JobId:       model.NewJobId("dongle-job"),
		RequestTime: time.Now(),
		Values: []*response.RegisterValue{
			response.NewRegisterValue(registers.DongleLoadPower, uint32(885)),
		},
	}
	if err := svc.StoreModbusValues(resp); err != nil {
		t.Fatalf("StoreModbusValues returned error: %v", err)
	}

	mu.Lock()
	defer mu.Unlock()
	if !strings.HasSuffix(requestPath, "/"+sharedconfig.StoreModbusValuesPath) {
		t.Errorf("path = %s, want suffix /%s", requestPath, sharedconfig.StoreModbusValuesPath)
	}
	jobId, ok := receivedBody["jobId"].(map[string]any)
	if !ok || jobId["id"] != "dongle-job" {
		t.Errorf("jobId = %v, want id dongle-job", receivedBody["jobId"])
	}
	values, ok := receivedBody["registerValues"].([]any)
	if !ok || len(values) != 1 {
		t.Fatalf("registerValues = %v, want one entry", receivedBody["registerValues"])
	}
}

func TestPersistenceService_StoreRawValues_FailsOnServerError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	svc := testService(t, server)

	if err := svc.StoreRawValues(&RawKpiRegisterValues{JobId: "j"}); err == nil {
		t.Error("expected error for HTTP 500, got nil (a failed store must fail the run)")
	}
}
