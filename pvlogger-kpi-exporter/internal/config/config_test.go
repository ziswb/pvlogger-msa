package config

import (
	"os"
	"testing"
)

func TestLoad_Defaults(t *testing.T) {
	t.Setenv("KPI_EXPORT_PATH", "/tmp/export")
	t.Setenv("KPI_FILE_PREFIX", "kpi")

	_ = os.Unsetenv("REST_HOST")
	_ = os.Unsetenv("REST_PORT")
	_ = os.Unsetenv("REST_PATH")
	_ = os.Unsetenv("REST_PROTOCOL")

	cfg := Load()

	if cfg.ExportPath != "/tmp/export" {
		t.Errorf("expected '/tmp/export', got '%s'", cfg.ExportPath)
	}
	if cfg.FilePrefix != "kpi" {
		t.Errorf("expected 'kpi', got '%s'", cfg.FilePrefix)
	}
	if cfg.RestHost != "localhost" {
		t.Errorf("expected default host 'localhost', got '%s'", cfg.RestHost)
	}
	if cfg.RestPort != "8083" {
		t.Errorf("expected default port '8083', got '%s'", cfg.RestPort)
	}
	if cfg.RestPath != "/v1/read-latest-kpi" {
		t.Errorf("expected default path, got '%s'", cfg.RestPath)
	}
	if cfg.RestProtocol != "http" {
		t.Errorf("expected default protocol 'http', got '%s'", cfg.RestProtocol)
	}
}

func TestLoad_CustomValues(t *testing.T) {
	t.Setenv("KPI_EXPORT_PATH", "/data/kpis")
	t.Setenv("KPI_FILE_PREFIX", "solar")
	t.Setenv("REST_HOST", "data-accessor")
	t.Setenv("REST_PORT", "9090")
	t.Setenv("REST_PATH", "/v2/kpi")
	t.Setenv("REST_PROTOCOL", "https")

	cfg := Load()

	if cfg.ExportPath != "/data/kpis" {
		t.Errorf("expected '/data/kpis', got '%s'", cfg.ExportPath)
	}
	if cfg.FilePrefix != "solar" {
		t.Errorf("expected 'solar', got '%s'", cfg.FilePrefix)
	}
	if cfg.RestHost != "data-accessor" {
		t.Errorf("expected 'data-accessor', got '%s'", cfg.RestHost)
	}
	if cfg.RestPort != "9090" {
		t.Errorf("expected '9090', got '%s'", cfg.RestPort)
	}
	if cfg.RestPath != "/v2/kpi" {
		t.Errorf("expected '/v2/kpi', got '%s'", cfg.RestPath)
	}
	if cfg.RestProtocol != "https" {
		t.Errorf("expected 'https', got '%s'", cfg.RestProtocol)
	}
}

// The env helper functions themselves are covered in pv-logger/config.
