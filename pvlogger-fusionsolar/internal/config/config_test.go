package config

import (
	"os"
	"testing"
)

func TestFsUrl(t *testing.T) {
	cfg := &Config{
		Fs: FusionSolarConfig{
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
		Fs: FusionSolarConfig{
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
		Fs: FusionSolarConfig{
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
	// Set required env vars
	t.Setenv("USERNAME", "testuser")
	t.Setenv("FS_STATION_CODE", "ST001")
	t.Setenv("FS_DEV_TYPE_ID", "1")
	t.Setenv("POSTGRES_PASSWORD", "secret")
	t.Setenv("POSTGRESQL_HOST", "localhost")

	// Clear optional vars to test defaults
	_ = os.Unsetenv("JOB_NAME")
	_ = os.Unsetenv("FS_HOST")
	_ = os.Unsetenv("FS_PATH")
	_ = os.Unsetenv("FS_PROTOCOL")
	_ = os.Unsetenv("POSTGRESQL_USERNAME")
	_ = os.Unsetenv("POSTGRESQL_PORT")
	_ = os.Unsetenv("DB_NAME")
	_ = os.Unsetenv("POSTGRESQL_SSLMODE")
	_ = os.Unsetenv("K8S_NAMESPACE")
	_ = os.Unsetenv("K8S_TOKEN_SECRET_NAME")
	_ = os.Unsetenv("K8S_TOKEN_KEY")
	_ = os.Unsetenv("USE_TOKEN_SECRET")

	cfg := Load()

	if cfg.Job.Name != "STATION_REAL_KPI" {
		t.Errorf("expected default job name 'STATION_REAL_KPI', got '%s'", cfg.Job.Name)
	}
	if cfg.Fs.Host != "eu5.fusionsolar.huawei.com" {
		t.Errorf("expected default host, got '%s'", cfg.Fs.Host)
	}
	if cfg.Fs.Path != "thirdData" {
		t.Errorf("expected default path 'thirdData', got '%s'", cfg.Fs.Path)
	}
	if cfg.Fs.Protocol != "https" {
		t.Errorf("expected default protocol 'https', got '%s'", cfg.Fs.Protocol)
	}
	if cfg.Database.Username != "postgres" {
		t.Errorf("expected default db username 'postgres', got '%s'", cfg.Database.Username)
	}
	if cfg.Database.Port != 5434 {
		t.Errorf("expected default port 5434, got %d", cfg.Database.Port)
	}
	if cfg.Database.DbName != "fsdata" {
		t.Errorf("expected default db name 'fsdata', got '%s'", cfg.Database.DbName)
	}
	if cfg.Database.SslMode != "prefer" {
		t.Errorf("expected default sslmode 'prefer', got '%s'", cfg.Database.SslMode)
	}
	if cfg.K8s.Namespace != "pvlogger" {
		t.Errorf("expected default namespace 'pvlogger', got '%s'", cfg.K8s.Namespace)
	}
	if cfg.K8s.TokenSecretName != "fusionsolar-token-secret" {
		t.Errorf("expected default token secret name, got '%s'", cfg.K8s.TokenSecretName)
	}
	if cfg.K8s.TokenKey != "FUSIONSOLAR_TOKEN" {
		t.Errorf("expected default token key, got '%s'", cfg.K8s.TokenKey)
	}
	if !cfg.K8s.UseTokenSecret {
		t.Error("expected UseTokenSecret=true by default")
	}
}

func TestLoad_CustomValues(t *testing.T) {
	t.Setenv("USERNAME", "myuser")
	t.Setenv("FS_STATION_CODE", "ST002")
	t.Setenv("FS_DEV_TYPE_ID", "2")
	t.Setenv("POSTGRES_PASSWORD", "pass123")
	t.Setenv("POSTGRESQL_HOST", "db.example.com")
	t.Setenv("JOB_NAME", "DEVICE_REAL_KPI")
	t.Setenv("FS_HOST", "custom.host.com")
	t.Setenv("POSTGRESQL_PORT", "5432")
	t.Setenv("POSTGRESQL_SSLMODE", "require")
	t.Setenv("USE_TOKEN_SECRET", "false")
	t.Setenv("FUSIONSOLAR_PASSWORD", "fspass")

	cfg := Load()

	if cfg.Job.Name != "DEVICE_REAL_KPI" {
		t.Errorf("expected job name 'DEVICE_REAL_KPI', got '%s'", cfg.Job.Name)
	}
	if cfg.Fs.Host != "custom.host.com" {
		t.Errorf("expected custom host, got '%s'", cfg.Fs.Host)
	}
	if cfg.Fs.Username != "myuser" {
		t.Errorf("expected username 'myuser', got '%s'", cfg.Fs.Username)
	}
	if cfg.Database.Port != 5432 {
		t.Errorf("expected port 5432, got %d", cfg.Database.Port)
	}
	if cfg.Database.SslMode != "require" {
		t.Errorf("expected sslmode 'require', got '%s'", cfg.Database.SslMode)
	}
	if cfg.K8s.UseTokenSecret {
		t.Error("expected UseTokenSecret=false")
	}
	if cfg.Fs.Password != "fspass" {
		t.Errorf("expected password 'fspass', got '%s'", cfg.Fs.Password)
	}
}

func TestLoad_PanicsOnMissingRequired(t *testing.T) {
	// Clear all required env vars
	_ = os.Unsetenv("USERNAME")
	_ = os.Unsetenv("FS_STATION_CODE")
	_ = os.Unsetenv("FS_DEV_TYPE_ID")
	_ = os.Unsetenv("POSTGRES_PASSWORD")
	_ = os.Unsetenv("POSTGRESQL_HOST")

	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic for missing required env vars")
		}
	}()

	Load()
}

func TestLoad_PasswordOptionalWhenUseTokenSecret(t *testing.T) {
	t.Setenv("USERNAME", "testuser")
	t.Setenv("FS_STATION_CODE", "ST001")
	t.Setenv("FS_DEV_TYPE_ID", "1")
	t.Setenv("POSTGRES_PASSWORD", "secret")
	t.Setenv("POSTGRESQL_HOST", "localhost")
	t.Setenv("USE_TOKEN_SECRET", "true")
	_ = os.Unsetenv("FUSIONSOLAR_PASSWORD")

	// Should not panic — password is optional when using token secret
	cfg := Load()
	if cfg.Fs.Password != "" {
		t.Errorf("expected empty password, got '%s'", cfg.Fs.Password)
	}
}

func TestLoad_PasswordRequiredWhenNotUseTokenSecret(t *testing.T) {
	t.Setenv("USERNAME", "testuser")
	t.Setenv("FS_STATION_CODE", "ST001")
	t.Setenv("FS_DEV_TYPE_ID", "1")
	t.Setenv("POSTGRES_PASSWORD", "secret")
	t.Setenv("POSTGRESQL_HOST", "localhost")
	t.Setenv("USE_TOKEN_SECRET", "false")
	_ = os.Unsetenv("FUSIONSOLAR_PASSWORD")

	defer func() {
		if r := recover(); r == nil {
			t.Error("expected panic when FUSIONSOLAR_PASSWORD is missing and USE_TOKEN_SECRET=false")
		}
	}()

	Load()
}

// The env helper functions themselves are covered in pv-logger/config.
