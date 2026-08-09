package config

import (
	"os"
	"testing"
)

func TestFsUrl(t *testing.T) {
	cfg := &Config{
		FusionSolar: FusionSolarConfig{
			Protocol: "https",
			Host:     "eu5.fusionsolar.huawei.com",
			Path:     "thirdData",
		},
	}
	expected := "https://eu5.fusionsolar.huawei.com/thirdData"
	if got := cfg.FsUrl(); got != expected {
		t.Errorf("expected %s, got %s", expected, got)
	}
}

func TestFsUrl_CustomValues(t *testing.T) {
	cfg := &Config{
		FusionSolar: FusionSolarConfig{
			Protocol: "http",
			Host:     "localhost:8080",
			Path:     "api/v1",
		},
	}
	expected := "http://localhost:8080/api/v1"
	if got := cfg.FsUrl(); got != expected {
		t.Errorf("expected %s, got %s", expected, got)
	}
}

func TestFsUrl_EmptyPath(t *testing.T) {
	cfg := &Config{
		FusionSolar: FusionSolarConfig{
			Protocol: "https",
			Host:     "example.com",
			Path:     "",
		},
	}
	expected := "https://example.com"
	if got := cfg.FsUrl(); got != expected {
		t.Errorf("expected %s, got %s", expected, got)
	}
}

func TestLoad_Defaults(t *testing.T) {
	t.Setenv("USERNAME", "testuser")
	t.Setenv("FUSIONSOLAR_PASSWORD", "testpass")

	_ = os.Unsetenv("FS_HOST")
	_ = os.Unsetenv("FS_PATH")
	_ = os.Unsetenv("FS_PROTOCOL")
	_ = os.Unsetenv("K8S_NAMESPACE")
	_ = os.Unsetenv("K8S_SECRET_NAME")
	_ = os.Unsetenv("K8S_TOKEN_KEY")

	cfg := Load()

	if cfg.FusionSolar.Host != "eu5.fusionsolar.huawei.com" {
		t.Errorf("expected default host, got '%s'", cfg.FusionSolar.Host)
	}
	if cfg.FusionSolar.Path != "thirdData" {
		t.Errorf("expected default path 'thirdData', got '%s'", cfg.FusionSolar.Path)
	}
	if cfg.FusionSolar.Protocol != "https" {
		t.Errorf("expected default protocol 'https', got '%s'", cfg.FusionSolar.Protocol)
	}
	if cfg.K8s.Namespace != "pvlogger" {
		t.Errorf("expected default namespace 'pvlogger', got '%s'", cfg.K8s.Namespace)
	}
	if cfg.K8s.SecretName != "fusionsolar-token-secret" {
		t.Errorf("expected default secret name, got '%s'", cfg.K8s.SecretName)
	}
	if cfg.K8s.TokenKey != "FUSIONSOLAR_TOKEN" {
		t.Errorf("expected default token key, got '%s'", cfg.K8s.TokenKey)
	}
}

func TestLoad_CustomValues(t *testing.T) {
	t.Setenv("USERNAME", "myuser")
	t.Setenv("FUSIONSOLAR_PASSWORD", "mypass")
	t.Setenv("FS_HOST", "custom.host.com")
	t.Setenv("FS_PATH", "api/v2")
	t.Setenv("FS_PROTOCOL", "http")
	t.Setenv("K8S_NAMESPACE", "custom-ns")
	t.Setenv("K8S_SECRET_NAME", "my-secret")
	t.Setenv("K8S_TOKEN_KEY", "MY_TOKEN")

	cfg := Load()

	if cfg.FusionSolar.Username != "myuser" {
		t.Errorf("expected 'myuser', got '%s'", cfg.FusionSolar.Username)
	}
	if cfg.FusionSolar.Password != "mypass" {
		t.Errorf("expected 'mypass', got '%s'", cfg.FusionSolar.Password)
	}
	if cfg.FusionSolar.Host != "custom.host.com" {
		t.Errorf("expected 'custom.host.com', got '%s'", cfg.FusionSolar.Host)
	}
	if cfg.FusionSolar.Path != "api/v2" {
		t.Errorf("expected 'api/v2', got '%s'", cfg.FusionSolar.Path)
	}
	if cfg.FusionSolar.Protocol != "http" {
		t.Errorf("expected 'http', got '%s'", cfg.FusionSolar.Protocol)
	}
	if cfg.K8s.Namespace != "custom-ns" {
		t.Errorf("expected 'custom-ns', got '%s'", cfg.K8s.Namespace)
	}
	if cfg.K8s.SecretName != "my-secret" {
		t.Errorf("expected 'my-secret', got '%s'", cfg.K8s.SecretName)
	}
	if cfg.K8s.TokenKey != "MY_TOKEN" {
		t.Errorf("expected 'MY_TOKEN', got '%s'", cfg.K8s.TokenKey)
	}
}

func TestLoad_PanicsOnMissingUsername(t *testing.T) {
	_ = os.Unsetenv("USERNAME")
	t.Setenv("FUSIONSOLAR_PASSWORD", "pass")

	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic for missing USERNAME")
		}
	}()
	Load()
}

func TestLoad_PanicsOnMissingPassword(t *testing.T) {
	t.Setenv("USERNAME", "user")
	_ = os.Unsetenv("FUSIONSOLAR_PASSWORD")

	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic for missing FUSIONSOLAR_PASSWORD")
		}
	}()
	Load()
}

// The env helper functions themselves are covered in pv-logger/config.
// The Load()-level tests above still exercise them end-to-end.
