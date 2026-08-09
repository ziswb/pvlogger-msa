package config

import (
	"os"
	"reflect"
	"testing"
)

func expectPanic(t *testing.T, name string) {
	t.Helper()
	if r := recover(); r == nil {
		t.Errorf("expected panic from %s", name)
	}
}

func TestGetEnv_FallbackUsed(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_NONEXISTENT")
	if got := GetEnv("TEST_ENV_NONEXISTENT", "fallback"); got != "fallback" {
		t.Errorf("expected 'fallback', got '%s'", got)
	}
}

func TestGetEnv_ValueOverridesFallback(t *testing.T) {
	t.Setenv("TEST_ENV_VAR", "actual")
	if got := GetEnv("TEST_ENV_VAR", "fallback"); got != "actual" {
		t.Errorf("expected 'actual', got '%s'", got)
	}
}

func TestGetEnvChecked_ReturnsValue(t *testing.T) {
	t.Setenv("TEST_ENV_CHECKED", "value123")
	if got := GetEnvChecked("TEST_ENV_CHECKED"); got != "value123" {
		t.Errorf("expected 'value123', got '%s'", got)
	}
}

func TestGetEnvChecked_PanicsOnMissing(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_MISSING")
	defer expectPanic(t, "GetEnvChecked")
	GetEnvChecked("TEST_ENV_MISSING")
}

func TestGetEnvChecked_PanicsOnEmpty(t *testing.T) {
	t.Setenv("TEST_ENV_EMPTY", "")
	defer expectPanic(t, "GetEnvChecked")
	GetEnvChecked("TEST_ENV_EMPTY")
}

func TestGetEnvOptional_MissingAllowedWhenOptional(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_OPTIONAL")
	if got := GetEnvOptional("TEST_ENV_OPTIONAL", true); got != "" {
		t.Errorf("expected empty string, got '%s'", got)
	}
}

func TestGetEnvOptional_PanicsWhenRequired(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_OPTIONAL")
	defer expectPanic(t, "GetEnvOptional")
	GetEnvOptional("TEST_ENV_OPTIONAL", false)
}

func TestGetIntEnv_ParsesValue(t *testing.T) {
	t.Setenv("TEST_ENV_INT", "8083")
	if got := GetIntEnv("TEST_ENV_INT", "1"); got != 8083 {
		t.Errorf("expected 8083, got %d", got)
	}
}

func TestGetIntEnv_FallbackUsed(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_INT_MISSING")
	if got := GetIntEnv("TEST_ENV_INT_MISSING", "42"); got != 42 {
		t.Errorf("expected 42, got %d", got)
	}
}

func TestGetIntEnv_PanicsOnNonInteger(t *testing.T) {
	t.Setenv("TEST_ENV_INT_BAD", "not-a-number")
	defer expectPanic(t, "GetIntEnv")
	GetIntEnv("TEST_ENV_INT_BAD", "1")
}

func TestGetBoolEnv_ParsesValue(t *testing.T) {
	t.Setenv("TEST_ENV_BOOL", "true")
	if got := GetBoolEnv("TEST_ENV_BOOL", "false"); got != true {
		t.Errorf("expected true, got %v", got)
	}
}

func TestGetBoolEnv_PanicsOnInvalid(t *testing.T) {
	t.Setenv("TEST_ENV_BOOL_BAD", "maybe")
	defer expectPanic(t, "GetBoolEnv")
	GetBoolEnv("TEST_ENV_BOOL_BAD", "false")
}

func TestGetUByteArrayEnv_ParsesList(t *testing.T) {
	t.Setenv("TEST_ENV_UBYTES", "1,2,16")
	got := GetUByteArrayEnv("TEST_ENV_UBYTES", "0")
	if !reflect.DeepEqual(got, []uint8{1, 2, 16}) {
		t.Errorf("expected [1 2 16], got %v", got)
	}
}

func TestGetUByteArrayEnv_FallbackUsed(t *testing.T) {
	_ = os.Unsetenv("TEST_ENV_UBYTES_MISSING")
	got := GetUByteArrayEnv("TEST_ENV_UBYTES_MISSING", "3,4")
	if !reflect.DeepEqual(got, []uint8{3, 4}) {
		t.Errorf("expected [3 4], got %v", got)
	}
}

func TestGetUByteArrayEnv_PanicsOnInvalid(t *testing.T) {
	t.Setenv("TEST_ENV_UBYTES_BAD", "1,x")
	defer expectPanic(t, "GetUByteArrayEnv")
	GetUByteArrayEnv("TEST_ENV_UBYTES_BAD", "0")
}
