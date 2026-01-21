package fusionsolar

import (
	"context"
	"log"
	"net/http"
	"pvlogger/internal/auth"
	"pvlogger/internal/database"
	"pvlogger/internal/model"

	"github.com/jackc/pgx/v5/pgtype"
)

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
			InstalledCapacity:  toPgTypeFloat8OrNil(d.DataItemMap.InstalledCapacity),
			InverterPower:      toPgTypeFloat8OrNil(d.DataItemMap.InverterPower),
			OngridPower:        toPgTypeFloat8OrNil(d.DataItemMap.OngridPower),
			PerformanceRatio:   toPgTypeFloat8OrNil(d.DataItemMap.PerformanceRatio),
			PerpowerRatio:      toPgTypeFloat8OrNil(d.DataItemMap.PerpowerRatio),
			PowerProfit:        toPgTypeFloat8OrNil(d.DataItemMap.PowerProfit),
			RadiationIntensity: toPgTypeFloat8OrNil(d.DataItemMap.RadiationIntensity),
			ReductionTotalCo2:  toPgTypeFloat8OrNil(d.DataItemMap.ReductionTotalCo2),
			ReductionTotalCoal: toPgTypeFloat8OrNil(d.DataItemMap.ReductionTotalCoal),
			ReductionTotalTree: toPgTypeFloat8OrNil(d.DataItemMap.ReductionTotalTree),
			TheoryPower:        toPgTypeFloat8OrNil(d.DataItemMap.TheoryPower),
			UsePower:           toPgTypeFloat8OrNil(d.DataItemMap.UsePower),
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

func toPgTypeFloat8OrNil(value *float32) pgtype.Float8 {
	if value == nil {
		return pgtype.Float8{Valid: false}
	}
	return pgtype.Float8{Valid: true, Float64: float64(*value)}
}

func messageAsPgTextOrNil(text *string) pgtype.Text {
	if text != nil {
		return pgtype.Text{Valid: true, String: *text}
	}
	return pgtype.Text{Valid: false}
}

func boolOrFalse(value *bool) bool {
	if value != nil {
		return *value
	}
	return false
}
