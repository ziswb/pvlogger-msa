package config

import (
	"os"
	"path/filepath"
	"regexp"
	"testing"
)

// The Kotlin route constants are the source of truth for REST paths: the
// servers compile their routes from them. This test fails the Go build when
// the Go-side path values drift from those constants (backlog item 9).

const (
	dataAccessorPathsKt = "../../pvlogger-shared-rest/src/main/kotlin/ch/bziswiler/pvlogger/shared/rest/DataAccessorPaths.kt"
	masterDataPathsKt   = "../../pvlogger-shared-rest/src/main/kotlin/ch/bziswiler/pvlogger/shared/rest/MasterDataPaths.kt"
)

var kotlinConstPattern = regexp.MustCompile(`const val (\w+)\s*=\s*"([^"]*)"`)

func parseKotlinStringConsts(t *testing.T, relPath string) map[string]string {
	t.Helper()
	path, err := filepath.Abs(relPath)
	if err != nil {
		t.Fatalf("failed to resolve %s: %v", relPath, err)
	}
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("failed to read Kotlin source %s: %v", path, err)
	}
	consts := make(map[string]string)
	for _, match := range kotlinConstPattern.FindAllStringSubmatch(string(content), -1) {
		consts[match[1]] = match[2]
	}
	if len(consts) == 0 {
		t.Fatalf("no `const val NAME = \"...\"` declarations found in %s — parser broken or file moved", path)
	}
	return consts
}

func TestDataAccessorPathsMatchKotlinConstants(t *testing.T) {
	kotlin := parseKotlinStringConsts(t, dataAccessorPathsKt)

	checks := []struct {
		kotlinName string
		goValue    string
	}{
		{"DATA_ACCESSOR_API_V1_PATH", DataAccessorBasePath},
		{"DATA_ACCESSOR_STORE_MODBUS_VALUES", StoreModbusValuesPath},
		{"DATA_ACCESSOR_STORE_KPI_VALUES", StoreKpiValuesPath},
		{"DATA_ACCESSOR_STORE_DEVICE_ID_VALUES", StoreDeviceIdValuesPath},
		{"DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES", StoreRawKpiRegisterValuesPath},
		{"DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH", ReadLatestRegisterValuePath},
		{"DATA_ACCESSOR_READ_METER_ENERGY_TODAY", ReadMeterEnergyTodayPath},
		{"DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH", ReadDayDataPath},
	}
	for _, c := range checks {
		kotlinValue, ok := kotlin[c.kotlinName]
		if !ok {
			t.Errorf("Kotlin constant %s not found in DataAccessorPaths.kt", c.kotlinName)
			continue
		}
		if c.goValue != kotlinValue {
			t.Errorf("Go path %q drifted from Kotlin %s = %q", c.goValue, c.kotlinName, kotlinValue)
		}
	}
}

func TestMasterDataPathMatchesKotlinConstants(t *testing.T) {
	kotlin := parseKotlinStringConsts(t, masterDataPathsKt)
	goValue := MasterDataPath

	// The Go client uses one path for both the list and the by-address
	// endpoint; the Kotlin side declares them as two constants.
	for _, kotlinName := range []string{
		"MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES",
		"MASTER_DATA_FOR_ADDRESS_PATH",
	} {
		kotlinValue, ok := kotlin[kotlinName]
		if !ok {
			t.Errorf("Kotlin constant %s not found in MasterDataPaths.kt", kotlinName)
			continue
		}
		if goValue != kotlinValue {
			t.Errorf("Go path %q drifted from Kotlin %s = %q", goValue, kotlinName, kotlinValue)
		}
	}
}
