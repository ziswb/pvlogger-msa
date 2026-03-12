package repository

import (
	"math"
	"testing"

	"github.com/jackc/pgx/v5/pgtype"
)

func TestToPgFloat8_Nil(t *testing.T) {
	result := ToPgFloat8(nil)
	if result.Valid {
		t.Error("expected Valid=false for nil input")
	}
}

func TestToPgFloat8_Value(t *testing.T) {
	v := float32(3.14)
	result := ToPgFloat8(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if math.Abs(result.Float64-float64(v)) > 1e-6 {
		t.Errorf("expected %f, got %f", float64(v), result.Float64)
	}
}

func TestToPgFloat8_Zero(t *testing.T) {
	v := float32(0)
	result := ToPgFloat8(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true for zero")
	}
	if result.Float64 != 0 {
		t.Errorf("expected 0, got %f", result.Float64)
	}
}

func TestToPgFloat8_Negative(t *testing.T) {
	v := float32(-42.5)
	result := ToPgFloat8(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if math.Abs(result.Float64-float64(v)) > 1e-6 {
		t.Errorf("expected %f, got %f", float64(v), result.Float64)
	}
}

func TestToPgInt4_Nil(t *testing.T) {
	result := ToPgInt4(nil)
	if result.Valid {
		t.Error("expected Valid=false for nil input")
	}
}

func TestToPgInt4_Value(t *testing.T) {
	v := 42
	result := ToPgInt4(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if result.Int32 != 42 {
		t.Errorf("expected 42, got %d", result.Int32)
	}
}

func TestToPgInt4_Zero(t *testing.T) {
	v := 0
	result := ToPgInt4(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true for zero")
	}
	if result.Int32 != 0 {
		t.Errorf("expected 0, got %d", result.Int32)
	}
}

func TestToPgInt4_Negative(t *testing.T) {
	v := -7
	result := ToPgInt4(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if result.Int32 != -7 {
		t.Errorf("expected -7, got %d", result.Int32)
	}
}

func TestToPgInt8_Nil(t *testing.T) {
	result := ToPgInt8(nil)
	if result.Valid {
		t.Error("expected Valid=false for nil input")
	}
}

func TestToPgInt8_Value(t *testing.T) {
	v := float32(1000)
	result := ToPgInt8(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if result.Int64 != 1000 {
		t.Errorf("expected 1000, got %d", result.Int64)
	}
}

func TestToPgInt8_TruncatesDecimal(t *testing.T) {
	v := float32(3.99)
	result := ToPgInt8(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if result.Int64 != 3 {
		t.Errorf("expected truncation to 3, got %d", result.Int64)
	}
}

func TestToPgText_Nil(t *testing.T) {
	result := ToPgText(nil)
	if result.Valid {
		t.Error("expected Valid=false for nil input")
	}
}

func TestToPgText_Value(t *testing.T) {
	v := "hello"
	result := ToPgText(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true")
	}
	if result.String != "hello" {
		t.Errorf("expected 'hello', got '%s'", result.String)
	}
}

func TestToPgText_Empty(t *testing.T) {
	v := ""
	result := ToPgText(&v)
	if !result.Valid {
		t.Fatal("expected Valid=true for empty string")
	}
	if result.String != "" {
		t.Errorf("expected empty string, got '%s'", result.String)
	}
}

func TestBoolOrFalse_Nil(t *testing.T) {
	if BoolOrFalse(nil) {
		t.Error("expected false for nil input")
	}
}

func TestBoolOrFalse_True(t *testing.T) {
	v := true
	if !BoolOrFalse(&v) {
		t.Error("expected true")
	}
}

func TestBoolOrFalse_False(t *testing.T) {
	v := false
	if BoolOrFalse(&v) {
		t.Error("expected false")
	}
}

func TestToPgFloat8_EqualsExpectedStruct(t *testing.T) {
	v := float32(1.5)
	result := ToPgFloat8(&v)
	expected := pgtype.Float8{Valid: true, Float64: 1.5}
	if result != expected {
		t.Errorf("expected %+v, got %+v", expected, result)
	}
}

func TestToPgInt4_EqualsExpectedStruct(t *testing.T) {
	v := 99
	result := ToPgInt4(&v)
	expected := pgtype.Int4{Valid: true, Int32: 99}
	if result != expected {
		t.Errorf("expected %+v, got %+v", expected, result)
	}
}

func TestToPgText_EqualsExpectedStruct(t *testing.T) {
	v := "test"
	result := ToPgText(&v)
	expected := pgtype.Text{Valid: true, String: "test"}
	if result != expected {
		t.Errorf("expected %+v, got %+v", expected, result)
	}
}
