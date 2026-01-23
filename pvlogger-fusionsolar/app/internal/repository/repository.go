package repository

import (
	"context"

	"github.com/jackc/pgx/v5/pgtype"
)

// FsResponseInput contains the data needed to insert a FusionSolar API response
type FsResponseInput struct {
	FailCode  int32
	Message   pgtype.Text
	Raw       []byte
	Success   bool
	Timestamp pgtype.Timestamp
}

// FsResponseParamsExtendedInput contains extended params for station KPIs
type FsResponseParamsExtendedInput struct {
	ResponseID   int32
	StationCodes pgtype.Text
	CurrentTime  pgtype.Int8
	CollectTime  pgtype.Int8
}

// FsResponseParamsBasicInput contains basic params for station real KPIs
type FsResponseParamsBasicInput struct {
	ResponseID   int32
	StationCodes pgtype.Text
	CurrentTime  pgtype.Int8
}

// FsResponseParamsDeviceInput contains params for device KPIs
type FsResponseParamsDeviceInput struct {
	ResponseID  int32
	DevIds      pgtype.Text
	DevTypeID   pgtype.Int4
	CurrentTime pgtype.Int8
}

// StationExtendedDataInput contains data for station extended KPIs (day/month/year)
type StationExtendedDataInput struct {
	CollectTime        int64
	Type               string
	StationCode        string
	InstalledCapacity  pgtype.Float8
	InverterPower      pgtype.Float8
	OngridPower        pgtype.Float8
	PerformanceRatio   pgtype.Float8
	PerpowerRatio      pgtype.Float8
	PowerProfit        pgtype.Float8
	RadiationIntensity pgtype.Float8
	ReductionTotalCo2  pgtype.Float8
	ReductionTotalCoal pgtype.Float8
	ReductionTotalTree pgtype.Float8
	TheoryPower        pgtype.Float8
	UsePower           pgtype.Float8
}

// StationExtendedDataMappingInput contains mapping data for station extended KPIs
type StationExtendedDataMappingInput struct {
	ResponseID  int32
	CollectTime int64
	Type        string
}

// StationRealKpiInput contains data for station real-time KPIs
type StationRealKpiInput struct {
	DayIncome       float64
	DayPower        float64
	MonthPower      float64
	RealHealthState int32
	Response        int32
	StationCode     pgtype.Text
	TotalIncome     float64
	TotalPower      float64
	Type            string
}

// DeviceRealKpiInput contains data for device real-time KPIs
type DeviceRealKpiInput struct {
	AI            pgtype.Float8
	AU            pgtype.Float8
	AbU           pgtype.Float8
	ActivePower   pgtype.Float8
	BI            pgtype.Float8
	BU            pgtype.Float8
	BcU           pgtype.Float8
	CI            pgtype.Float8
	CU            pgtype.Float8
	CaU           pgtype.Float8
	CloseTime     pgtype.Int8
	DayCap        pgtype.Float8
	DevId         pgtype.Int8
	Efficiency    pgtype.Float8
	ElecFreq      pgtype.Float8
	InverterState pgtype.Float8
	Mppt10Cap     pgtype.Float8
	Mppt1Cap      pgtype.Float8
	Mppt2Cap      pgtype.Float8
	Mppt3Cap      pgtype.Float8
	Mppt4Cap      pgtype.Float8
	Mppt5Cap      pgtype.Float8
	Mppt6Cap      pgtype.Float8
	Mppt7Cap      pgtype.Float8
	Mppt8Cap      pgtype.Float8
	Mppt9Cap      pgtype.Float8
	MpptPower     pgtype.Float8
	MpptTotalCap  pgtype.Float8
	OpenTime      pgtype.Int8
	PowerFactor   pgtype.Float8
	Pv10I         pgtype.Float8
	Pv10U         pgtype.Float8
	Pv11I         pgtype.Float8
	Pv11U         pgtype.Float8
	Pv12I         pgtype.Float8
	Pv12U         pgtype.Float8
	Pv13I         pgtype.Float8
	Pv13U         pgtype.Float8
	Pv14I         pgtype.Float8
	Pv14U         pgtype.Float8
	Pv15I         pgtype.Float8
	Pv15U         pgtype.Float8
	Pv16I         pgtype.Float8
	Pv16U         pgtype.Float8
	Pv17I         pgtype.Float8
	Pv17U         pgtype.Float8
	Pv18I         pgtype.Float8
	Pv18U         pgtype.Float8
	Pv19I         pgtype.Float8
	Pv19U         pgtype.Float8
	Pv1I          pgtype.Float8
	Pv1U          pgtype.Float8
	Pv20I         pgtype.Float8
	Pv20U         pgtype.Float8
	Pv21I         pgtype.Float8
	Pv21U         pgtype.Float8
	Pv22I         pgtype.Float8
	Pv22U         pgtype.Float8
	Pv23I         pgtype.Float8
	Pv23U         pgtype.Float8
	Pv24I         pgtype.Float8
	Pv24U         pgtype.Float8
	Pv25I         pgtype.Float8
	Pv25U         pgtype.Float8
	Pv26I         pgtype.Float8
	Pv26U         pgtype.Float8
	Pv27I         pgtype.Float8
	Pv27U         pgtype.Float8
	Pv28I         pgtype.Float8
	Pv28U         pgtype.Float8
	Pv2I          pgtype.Float8
	Pv2U          pgtype.Float8
	Pv3I          pgtype.Float8
	Pv3U          pgtype.Float8
	Pv4I          pgtype.Float8
	Pv4U          pgtype.Float8
	Pv5I          pgtype.Float8
	Pv5U          pgtype.Float8
	Pv6I          pgtype.Float8
	Pv6U          pgtype.Float8
	Pv7I          pgtype.Float8
	Pv7U          pgtype.Float8
	Pv8I          pgtype.Float8
	Pv8U          pgtype.Float8
	Pv9I          pgtype.Float8
	Pv9U          pgtype.Float8
	ReactivePower pgtype.Float8
	Response      int32
	RunState      pgtype.Int4
	Temperature   pgtype.Float8
	TotalCap      pgtype.Float8
	Type          string
}

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
