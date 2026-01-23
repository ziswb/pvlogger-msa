package fusionsolar

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
)

// FsResponseData provides a common interface for extracting response metadata
type FsResponseData interface {
	GetBody() []byte
	GetFailCode() int
	GetMessage() *string
	GetSuccess() *bool
}

// Response adapters for generated types

type kpiStationDayResponseAdapter struct {
	*model.KpiStationDayResponse
}

func (r kpiStationDayResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationDayResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationDayResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationDayResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type kpiStationMonthResponseAdapter struct {
	*model.KpiStationMonthResponse
}

func (r kpiStationMonthResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationMonthResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationMonthResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationMonthResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type kpiStationYearResponseAdapter struct {
	*model.KpiStationYearResponse
}

func (r kpiStationYearResponseAdapter) GetBody() []byte     { return r.Body }
func (r kpiStationYearResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r kpiStationYearResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r kpiStationYearResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type stationRealKpiResponseAdapter struct {
	*model.StationRealKpiResponse
}

func (r stationRealKpiResponseAdapter) GetBody() []byte     { return r.Body }
func (r stationRealKpiResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r stationRealKpiResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r stationRealKpiResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

type deviceRealKpiResponseAdapter struct {
	*model.DeviceRealKpiResponse
}

func (r deviceRealKpiResponseAdapter) GetBody() []byte     { return r.Body }
func (r deviceRealKpiResponseAdapter) GetFailCode() int    { return r.JSON200.FailCode }
func (r deviceRealKpiResponseAdapter) GetMessage() *string { return r.JSON200.Message }
func (r deviceRealKpiResponseAdapter) GetSuccess() *bool   { return r.JSON200.Success }

// insertStationExtendedParams inserts common extended params for station KPIs (Day/Month/Year)
func (f *FusionSolarAccess) insertStationExtendedParams(responseId int32, stationCodes *string, currentTime *int64, collectTime *int64) error {
	sc := pgtype.Text{Valid: true, String: *stationCodes}
	ct := pgtype.Int8{Valid: true, Int64: *currentTime}
	clt := pgtype.Int8{Valid: true, Int64: *collectTime}
	return f.insertFsResponseParamsExtended(responseId, sc, ct, clt)
}

// insertResponseFromData extracts common response fields and inserts them into the database
func (f *FusionSolarAccess) insertResponseFromData(resp FsResponseData) (int32, error) {
	log.Default().Println("Saving response params to database...")
	raw, err := json.RawMessage.MarshalJSON(resp.GetBody())
	if err != nil {
		return 0, err
	}
	failCode := int32(resp.GetFailCode())
	message := toPgText(resp.GetMessage())
	success := boolOrFalse(resp.GetSuccess())
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	return f.insertFsResponseParams(failCode, message, raw, success, timestamp)
}

func (f *FusionSolarAccess) insertFsResponseParams(failCode int32, message pgtype.Text, raw []byte, success bool, timestamp pgtype.Timestamp) (int32, error) {
	log.Default().Println("Saving fs response params to database...")
	// saveGenericResponse
	responseParams := database.InsertFsResponseParams{
		Timestamp: timestamp,
		Success:   success,
		FailCode:  failCode,
		Message:   message,
		Raw:       raw,
	}
	insertedFsResponse, err := database.New(f.Dao.Database.Conn).InsertFsResponse(f.Dao.Database.Ctx, responseParams)
	if err != nil {
		return 0, err
	}
	log.Default().Println("Fs response params saved successfully.")
	return insertedFsResponse.ID, nil
}

func WithBearerToken(token auth.Token) model.ClientOption {
	return model.WithRequestEditorFn(func(ctx context.Context, req *http.Request) error {
		log.Default().Println("Adding token to request...")
		req.Header.Set("XSRF-TOKEN", token.Token)
		return nil
	})
}

func (f *FusionSolarAccess) newClient() (*model.ClientWithResponses, error) {
	return model.NewClientWithResponses(config.FsUrl(), WithBearerToken(f.token))
}

func (f *FusionSolarAccess) upsertExtendedKpis(responseId int32, typ model.FusionSolarDataType, data []struct {
	CollectTime int64 `json:"collectTime"`
	DataItemMap struct {
		InstalledCapacity  *float32 `json:"installed_capacity,omitempty"`
		InverterPower      *float32 `json:"inverter_power,omitempty"`
		OngridPower        *float32 `json:"ongrid_power,omitempty"`
		PerformanceRatio   *float32 `json:"performance_ratio,omitempty"`
		PerpowerRatio      *float32 `json:"perpower_ratio,omitempty"`
		PowerProfit        *float32 `json:"power_profit,omitempty"`
		RadiationIntensity *float32 `json:"radiation_intensity,omitempty"`
		ReductionTotalCo2  *float32 `json:"reduction_total_co2,omitempty"`
		ReductionTotalCoal *float32 `json:"reduction_total_coal,omitempty"`
		ReductionTotalTree *float32 `json:"reduction_total_tree,omitempty"`
		TheoryPower        *float32 `json:"theory_power,omitempty"`
		UsePower           *float32 `json:"use_power,omitempty"`
	} `json:"dataItemMap"`
	StationCode string `json:"stationCode"`
}) error {
	log.Default().Println("Saving extended kpis to database...")
	for _, d := range data {
		extendedDataParams := database.UpsertFsStationExtendedDataParams{
			CollectTime:        d.CollectTime,
			Type:               typ.String(),
			StationCode:        d.StationCode,
			InstalledCapacity:  toPgFloat8(d.DataItemMap.InstalledCapacity),
			InverterPower:      toPgFloat8(d.DataItemMap.InverterPower),
			OngridPower:        toPgFloat8(d.DataItemMap.OngridPower),
			PerformanceRatio:   toPgFloat8(d.DataItemMap.PerformanceRatio),
			PerpowerRatio:      toPgFloat8(d.DataItemMap.PerpowerRatio),
			PowerProfit:        toPgFloat8(d.DataItemMap.PowerProfit),
			RadiationIntensity: toPgFloat8(d.DataItemMap.RadiationIntensity),
			ReductionTotalCo2:  toPgFloat8(d.DataItemMap.ReductionTotalCo2),
			ReductionTotalCoal: toPgFloat8(d.DataItemMap.ReductionTotalCoal),
			ReductionTotalTree: toPgFloat8(d.DataItemMap.ReductionTotalTree),
			TheoryPower:        toPgFloat8(d.DataItemMap.TheoryPower),
			UsePower:           toPgFloat8(d.DataItemMap.UsePower),
		}
		err := database.New(f.Dao.Database.Conn).UpsertFsStationExtendedData(f.Dao.Database.Ctx, extendedDataParams)
		if err != nil {
			return err
		}
		log.Default().Println("Extended kpis: saved params successfully.")
		log.Default().Println("Extended kpis: saving mapping params...")
		mappingParams := database.InsertFsStationExtendedDataMappingParams{
			ResponseID:  responseId,
			CollectTime: d.CollectTime,
			Type:        typ.String(),
		}
		err = database.New(f.Dao.Database.Conn).InsertFsStationExtendedDataMapping(f.Dao.Database.Ctx, mappingParams)
		if err != nil {
			return err
		}
		log.Default().Println("Extended kpis: saved mapping params successfully.")
	}
	return nil
}

func (f *FusionSolarAccess) insertFsResponseParamsExtended(responseId int32, stationCodes pgtype.Text, currentTime pgtype.Int8, collectTime pgtype.Int8) error {
	log.Default().Println("Saving Extended Kpi Basic Params  to database...")
	extendedParams := database.InsertFsResponseParamsExtendedParams{
		Response:     responseId,
		StationCodes: stationCodes,
		CurrentTime:  currentTime,
		CollectTime:  collectTime,
	}
	err := database.New(f.Dao.Database.Conn).InsertFsResponseParamsExtended(f.Dao.Database.Ctx, extendedParams)
	if err != nil {
		return err
	}
	log.Default().Println("Station Extended Kpi Basic Params saved successfully.")
	return nil
}

func toPgFloat8(value *float32) pgtype.Float8 {
	if value == nil {
		return pgtype.Float8{Valid: false}
	}
	return pgtype.Float8{Valid: true, Float64: float64(*value)}
}

func toPgInt4(value *int) pgtype.Int4 {
	if value == nil {
		return pgtype.Int4{Valid: false}
	}
	return pgtype.Int4{Valid: true, Int32: int32(*value)}
}

func toPgInt8(value *int64) pgtype.Int8 {
	if value == nil {
		return pgtype.Int8{Valid: false}
	}
	return pgtype.Int8{Valid: true, Int64: *value}
}

func toPgText(value *string) pgtype.Text {
	if value == nil {
		return pgtype.Text{Valid: false}
	}
	return pgtype.Text{Valid: true, String: *value}
}

func boolOrFalse(value *bool) bool {
	if value != nil {
		return *value
	}
	return false
}
