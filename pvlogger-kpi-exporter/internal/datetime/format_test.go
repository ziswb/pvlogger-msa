package datetime

import (
	"os"
	"strings"
	"testing"
)

func TestMain(m *testing.M) {
	os.Setenv("TZ", "Europe/Zurich")
	os.Exit(m.Run())
}

func TestFormatForDataRecord_KnownTimestamp(t *testing.T) {
	// Unix timestamp 0 in Europe/Zurich (CET = UTC+1) is 1970-01-01 01:00:00
	result := FormatForDataRecord(0)

	if result != "1970-01-01 01:00:00" {
		t.Errorf("expected '1970-01-01 01:00:00', got '%s'", result)
	}
}

func TestFormatForDataRecord_Format(t *testing.T) {
	// Verify the output matches the expected format pattern
	result := FormatForDataRecord(1709208000000)

	// Should have format "YYYY-MM-DD HH:MM:SS"
	if len(result) != 19 {
		t.Errorf("expected length 19, got %d for '%s'", len(result), result)
	}
	if result[4] != '-' || result[7] != '-' || result[10] != ' ' || result[13] != ':' || result[16] != ':' {
		t.Errorf("unexpected format: '%s'", result)
	}
}

func TestFormatFileNameSafe_KnownTimestamp(t *testing.T) {
	// Unix timestamp 0 in Europe/Zurich (CET = UTC+1) is 1970-01-01 01:00:00
	result := FormatFileNameSafe(0)

	if result != "1970-01-01_01-00-00.000" {
		t.Errorf("expected '1970-01-01_01-00-00.000', got '%s'", result)
	}
}

func TestFormatFileNameSafe_Format(t *testing.T) {
	result := FormatFileNameSafe(1709208000000)

	// Should have format "YYYY-MM-DD_HH-MM-SS.mmm"
	if len(result) != 23 {
		t.Errorf("expected length 23, got %d for '%s'", len(result), result)
	}
	if result[4] != '-' || result[7] != '-' || result[10] != '_' || result[13] != '-' || result[16] != '-' || result[19] != '.' {
		t.Errorf("unexpected format: '%s'", result)
	}
}

func TestFormatFileNameSafe_NoUnsafeChars(t *testing.T) {
	result := FormatFileNameSafe(1709208000000)

	// File-safe: no colons or spaces
	if strings.ContainsAny(result, ": /\\") {
		t.Errorf("filename contains unsafe characters: '%s'", result)
	}
}

func TestFormatFileNameSafe_MillisecondPrecision(t *testing.T) {
	result1 := FormatFileNameSafe(1709208000000)
	result2 := FormatFileNameSafe(1709208000123)

	if result1 == result2 {
		t.Error("expected different results for timestamps differing by milliseconds")
	}

	// Check millisecond part
	if !strings.HasSuffix(result1, ".000") {
		t.Errorf("expected .000 suffix, got '%s'", result1)
	}
	if !strings.HasSuffix(result2, ".123") {
		t.Errorf("expected .123 suffix, got '%s'", result2)
	}
}

func TestFormatForDataRecord_KnownTimestampWithValue(t *testing.T) {
	// 1709208000000 ms = 2024-02-29 12:00:00 UTC = 2024-02-29 13:00:00 CET
	result := FormatForDataRecord(1709208000000)

	if result != "2024-02-29 13:00:00" {
		t.Errorf("expected '2024-02-29 13:00:00', got '%s'", result)
	}
}

func TestFormatForDataRecord_ConsistentWithFileNameSafe(t *testing.T) {
	ts := int64(1709208000000)
	record := FormatForDataRecord(ts)
	fileName := FormatFileNameSafe(ts)

	// Both should produce the same date part
	recordDate := record[:10]
	fileDate := fileName[:10]
	if recordDate != fileDate {
		t.Errorf("date mismatch: record='%s', fileName='%s'", recordDate, fileDate)
	}
}
