package repository

import (
	"context"
	"pvlogger-fusionsolar/internal/database"
	"time"

	"pv-logger/logging"
)

// PostgresRepository implements Repository using PostgreSQL via sqlc-generated queries
type PostgresRepository struct {
	db *database.Database
}

// NewPostgresRepository creates a new PostgresRepository
func NewPostgresRepository(db *database.Database) *PostgresRepository {
	return &PostgresRepository{db: db}
}

func (r *PostgresRepository) queries() *database.Queries {
	return database.New(r.db.Conn)
}

func (r *PostgresRepository) SelectFormer5MinDeviceRealKpi(ctx context.Context) (map[time.Time]int, error) {
	kpi, err := r.queries().SelectFormer5MinDeviceRealKpi(ctx)
	if err != nil {
		return nil, err
	}
	dts := make(map[time.Time]int)
	for _, dt := range kpi {
		t := time.UnixMilli(dt.Int64)
		d := time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, time.UTC)
		dts[d] = dts[d] + 1
	}
	return dts, nil
}

func (r *PostgresRepository) InsertFsResponse(ctx context.Context, input FsResponseInput) (int32, error) {
	logging.Info("inserting fs response")
	params := database.InsertFsResponseParams{
		FailCode:  input.FailCode,
		Message:   input.Message,
		Raw:       string(input.Raw),
		Success:   input.Success,
		Timestamp: input.Timestamp,
	}
	result, err := r.queries().InsertFsResponse(ctx, params)
	if err != nil {
		return 0, err
	}
	logging.Info("fs response inserted successfully", "id", result.ID)
	return result.ID, nil
}

func (r *PostgresRepository) InsertFsResponseParamsExtended(ctx context.Context, input FsResponseParamsExtendedInput) error {
	logging.Info("inserting fs response params extended")
	params := database.InsertFsResponseParamsExtendedParams{
		Response:     input.ResponseID,
		StationCodes: input.StationCodes,
		CurrentTime:  input.CurrentTime,
		CollectTime:  input.CollectTime,
	}
	err := r.queries().InsertFsResponseParamsExtended(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("fs response params extended inserted successfully")
	return nil
}

func (r *PostgresRepository) InsertFsResponseParamsBasic(ctx context.Context, input FsResponseParamsBasicInput) error {
	logging.Info("inserting fs response params basic")
	params := database.InsertFsResponseParamsBasicParams{
		Response:     input.ResponseID,
		StationCodes: input.StationCodes,
		CurrentTime:  input.CurrentTime,
	}
	err := r.queries().InsertFsResponseParamsBasic(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("fs response params basic inserted successfully")
	return nil
}

func (r *PostgresRepository) InsertFsResponseParamsDevice(ctx context.Context, input FsResponseParamsDeviceInput) error {
	logging.Info("inserting fs response params device")
	params := database.InsertFsResponseParamsDeviceParams{
		Response:    input.ResponseID,
		DevIds:      input.DevIds,
		DevTypeID:   input.DevTypeID,
		CurrentTime: input.CurrentTime,
	}
	err := r.queries().InsertFsResponseParamsDevice(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("fs response params device inserted successfully")
	return nil
}

func (r *PostgresRepository) UpsertStationExtendedData(ctx context.Context, input StationExtendedDataInput) error {
	logging.Info("upserting station extended data")
	params := database.UpsertFsStationExtendedDataParams{
		CollectTime:        input.CollectTime,
		Type:               input.Type,
		StationCode:        input.StationCode,
		InstalledCapacity:  input.InstalledCapacity,
		InverterPower:      input.InverterPower,
		OngridPower:        input.OngridPower,
		PerformanceRatio:   input.PerformanceRatio,
		PerpowerRatio:      input.PerpowerRatio,
		PowerProfit:        input.PowerProfit,
		RadiationIntensity: input.RadiationIntensity,
		ReductionTotalCo2:  input.ReductionTotalCo2,
		ReductionTotalCoal: input.ReductionTotalCoal,
		ReductionTotalTree: input.ReductionTotalTree,
		TheoryPower:        input.TheoryPower,
		UsePower:           input.UsePower,
	}
	err := r.queries().UpsertFsStationExtendedData(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("station extended data upserted successfully")
	return nil
}

func (r *PostgresRepository) InsertStationExtendedDataMapping(ctx context.Context, input StationExtendedDataMappingInput) error {
	logging.Info("inserting station extended data mapping")
	params := database.InsertFsStationExtendedDataMappingParams{
		ResponseID:  input.ResponseID,
		CollectTime: input.CollectTime,
		Type:        input.Type,
	}
	err := r.queries().InsertFsStationExtendedDataMapping(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("station extended data mapping inserted successfully")
	return nil
}

func (r *PostgresRepository) InsertStationRealKpi(ctx context.Context, input StationRealKpiInput) error {
	logging.Info("inserting station real KPI")
	params := database.InsertFsStationRealKpiParams{
		DayIncome:       input.DayIncome,
		DayPower:        input.DayPower,
		MonthPower:      input.MonthPower,
		RealHealthState: input.RealHealthState,
		Response:        input.Response,
		StationCode:     input.StationCode,
		TotalIncome:     input.TotalIncome,
		TotalPower:      input.TotalPower,
		Type:            input.Type,
	}
	err := r.queries().InsertFsStationRealKpi(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("station real KPI inserted successfully")
	return nil
}

func (r *PostgresRepository) InsertDeviceRealKpi(ctx context.Context, input DeviceRealKpiInput) error {
	logging.Info("inserting device real KPI")
	params := database.InsertFsDeviceRealKpiParams{
		AI:            input.AI,
		AU:            input.AU,
		AbU:           input.AbU,
		ActivePower:   input.ActivePower,
		BI:            input.BI,
		BU:            input.BU,
		BcU:           input.BcU,
		CI:            input.CI,
		CU:            input.CU,
		CaU:           input.CaU,
		CloseTime:     input.CloseTime,
		DayCap:        input.DayCap,
		DevId:         input.DevId,
		Efficiency:    input.Efficiency,
		ElecFreq:      input.ElecFreq,
		InverterState: input.InverterState,
		Mppt10Cap:     input.Mppt10Cap,
		Mppt1Cap:      input.Mppt1Cap,
		Mppt2Cap:      input.Mppt2Cap,
		Mppt3Cap:      input.Mppt3Cap,
		Mppt4Cap:      input.Mppt4Cap,
		Mppt5Cap:      input.Mppt5Cap,
		Mppt6Cap:      input.Mppt6Cap,
		Mppt7Cap:      input.Mppt7Cap,
		Mppt8Cap:      input.Mppt8Cap,
		Mppt9Cap:      input.Mppt9Cap,
		MpptPower:     input.MpptPower,
		MpptTotalCap:  input.MpptTotalCap,
		OpenTime:      input.OpenTime,
		PowerFactor:   input.PowerFactor,
		Pv10I:         input.Pv10I,
		Pv10U:         input.Pv10U,
		Pv11I:         input.Pv11I,
		Pv11U:         input.Pv11U,
		Pv12I:         input.Pv12I,
		Pv12U:         input.Pv12U,
		Pv13I:         input.Pv13I,
		Pv13U:         input.Pv13U,
		Pv14I:         input.Pv14I,
		Pv14U:         input.Pv14U,
		Pv15I:         input.Pv15I,
		Pv15U:         input.Pv15U,
		Pv16I:         input.Pv16I,
		Pv16U:         input.Pv16U,
		Pv17I:         input.Pv17I,
		Pv17U:         input.Pv17U,
		Pv18I:         input.Pv18I,
		Pv18U:         input.Pv18U,
		Pv19I:         input.Pv19I,
		Pv19U:         input.Pv19U,
		Pv1I:          input.Pv1I,
		Pv1U:          input.Pv1U,
		Pv20I:         input.Pv20I,
		Pv20U:         input.Pv20U,
		Pv21I:         input.Pv21I,
		Pv21U:         input.Pv21U,
		Pv22I:         input.Pv22I,
		Pv22U:         input.Pv22U,
		Pv23I:         input.Pv23I,
		Pv23U:         input.Pv23U,
		Pv24I:         input.Pv24I,
		Pv24U:         input.Pv24U,
		Pv25I:         input.Pv25I,
		Pv25U:         input.Pv25U,
		Pv26I:         input.Pv26I,
		Pv26U:         input.Pv26U,
		Pv27I:         input.Pv27I,
		Pv27U:         input.Pv27U,
		Pv28I:         input.Pv28I,
		Pv28U:         input.Pv28U,
		Pv2I:          input.Pv2I,
		Pv2U:          input.Pv2U,
		Pv3I:          input.Pv3I,
		Pv3U:          input.Pv3U,
		Pv4I:          input.Pv4I,
		Pv4U:          input.Pv4U,
		Pv5I:          input.Pv5I,
		Pv5U:          input.Pv5U,
		Pv6I:          input.Pv6I,
		Pv6U:          input.Pv6U,
		Pv7I:          input.Pv7I,
		Pv7U:          input.Pv7U,
		Pv8I:          input.Pv8I,
		Pv8U:          input.Pv8U,
		Pv9I:          input.Pv9I,
		Pv9U:          input.Pv9U,
		ReactivePower: input.ReactivePower,
		Response:      input.Response,
		RunState:      input.RunState,
		Temperature:   input.Temperature,
		TotalCap:      input.TotalCap,
		Type:          input.Type,
	}
	err := r.queries().InsertFsDeviceRealKpi(ctx, params)
	if err != nil {
		return err
	}
	logging.Info("device real KPI inserted successfully")
	return nil
}
