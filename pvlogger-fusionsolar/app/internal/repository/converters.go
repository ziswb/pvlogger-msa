package repository

import (
	"github.com/jackc/pgx/v5/pgtype"
)

// ToPgFloat8 converts a *float32 to pgtype.Float8
func ToPgFloat8(value *float32) pgtype.Float8 {
	if value == nil {
		return pgtype.Float8{Valid: false}
	}
	return pgtype.Float8{Valid: true, Float64: float64(*value)}
}

// ToPgInt4 converts a *int to pgtype.Int4
func ToPgInt4(value *int) pgtype.Int4 {
	if value == nil {
		return pgtype.Int4{Valid: false}
	}
	return pgtype.Int4{Valid: true, Int32: int32(*value)}
}

// ToPgInt8 converts a *int64 to pgtype.Int8
func ToPgInt8(value *int64) pgtype.Int8 {
	if value == nil {
		return pgtype.Int8{Valid: false}
	}
	return pgtype.Int8{Valid: true, Int64: *value}
}

// ToPgText converts a *string to pgtype.Text
func ToPgText(value *string) pgtype.Text {
	if value == nil {
		return pgtype.Text{Valid: false}
	}
	return pgtype.Text{Valid: true, String: *value}
}

// BoolOrFalse returns the value of a *bool or false if nil
func BoolOrFalse(value *bool) bool {
	if value != nil {
		return *value
	}
	return false
}
