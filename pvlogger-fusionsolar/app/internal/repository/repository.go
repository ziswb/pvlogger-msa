package repository

import (
	"context"
)

// Repository defines the interface for FusionSolar data persistence operations
type Repository interface {
	// InsertFsResponse inserts a FusionSolar API response and returns the generated ID
	InsertFsResponse(ctx context.Context, input FsResponseInput) (int32, error)

	// InsertFsResponseParamsExtended inserts extended params for station KPIs (day/month/year)
	InsertFsResponseParamsExtended(ctx context.Context, input FsResponseParamsExtendedInput) error

	// InsertFsResponseParamsBasic inserts basic params for station real KPIs
	InsertFsResponseParamsBasic(ctx context.Context, input FsResponseParamsBasicInput) error

	// InsertFsResponseParamsDevice inserts params for device KPIs
	InsertFsResponseParamsDevice(ctx context.Context, input FsResponseParamsDeviceInput) error

	// UpsertStationExtendedData upserts station extended KPI data
	UpsertStationExtendedData(ctx context.Context, input StationExtendedDataInput) error

	// InsertStationExtendedDataMapping inserts a mapping between response and extended data
	InsertStationExtendedDataMapping(ctx context.Context, input StationExtendedDataMappingInput) error

	// InsertStationRealKpi inserts station real-time KPI data
	InsertStationRealKpi(ctx context.Context, input StationRealKpiInput) error

	// InsertDeviceRealKpi inserts device real-time KPI data
	InsertDeviceRealKpi(ctx context.Context, input DeviceRealKpiInput) error
}
