package fusionsolar

import (
	"encoding/json"
	"pvlogger-fusionsolar/internal/model"
	"pvlogger-fusionsolar/internal/repository"
	"time"

	"pvlogger-go-shared/logging"

	"github.com/jackc/pgx/v5/pgtype"
)

// insertStationExtendedParams inserts common extended params for station KPIs (Day/Month/Year)
func (f *FusionSolarAccess) insertStationExtendedParams(responseId int32, stationCodes *string, currentTime *int64, collectTime *int64) error {
	input := repository.FsResponseParamsExtendedInput{
		ResponseID:   responseId,
		StationCodes: pgtype.Text{Valid: true, String: *stationCodes},
		CurrentTime:  pgtype.Int8{Valid: true, Int64: *currentTime},
		CollectTime:  pgtype.Int8{Valid: true, Int64: *collectTime},
	}
	return f.repo.InsertFsResponseParamsExtended(f.db.Ctx, input)
}

// insertResponseFromData extracts common response fields and inserts them into the database
func (f *FusionSolarAccess) insertResponseFromData(resp FsResponseData) (int32, error) {
	logging.Debug("saving response params to database")
	raw, err := json.RawMessage.MarshalJSON(resp.GetBody())
	if err != nil {
		return 0, err
	}
	input := repository.FsResponseInput{
		FailCode:  int32(resp.GetFailCode()),
		Message:   repository.ToPgText(resp.GetMessage()),
		Raw:       raw,
		Success:   repository.BoolOrFalse(resp.GetSuccess()),
		Timestamp: pgtype.Timestamp{Valid: true, Time: time.Now()},
	}
	return f.repo.InsertFsResponse(f.db.Ctx, input)
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
	logging.Debug("saving extended KPIs to database")
	for _, d := range data {
		extendedDataInput := repository.StationExtendedDataInput{
			CollectTime:        d.CollectTime,
			Type:               typ.String(),
			StationCode:        d.StationCode,
			InstalledCapacity:  repository.ToPgFloat8(d.DataItemMap.InstalledCapacity),
			InverterPower:      repository.ToPgFloat8(d.DataItemMap.InverterPower),
			OngridPower:        repository.ToPgFloat8(d.DataItemMap.OngridPower),
			PerformanceRatio:   repository.ToPgFloat8(d.DataItemMap.PerformanceRatio),
			PerpowerRatio:      repository.ToPgFloat8(d.DataItemMap.PerpowerRatio),
			PowerProfit:        repository.ToPgFloat8(d.DataItemMap.PowerProfit),
			RadiationIntensity: repository.ToPgFloat8(d.DataItemMap.RadiationIntensity),
			ReductionTotalCo2:  repository.ToPgFloat8(d.DataItemMap.ReductionTotalCo2),
			ReductionTotalCoal: repository.ToPgFloat8(d.DataItemMap.ReductionTotalCoal),
			ReductionTotalTree: repository.ToPgFloat8(d.DataItemMap.ReductionTotalTree),
			TheoryPower:        repository.ToPgFloat8(d.DataItemMap.TheoryPower),
			UsePower:           repository.ToPgFloat8(d.DataItemMap.UsePower),
		}
		err := f.repo.UpsertStationExtendedData(f.db.Ctx, extendedDataInput)
		if err != nil {
			return err
		}
		mappingInput := repository.StationExtendedDataMappingInput{
			ResponseID:  responseId,
			CollectTime: d.CollectTime,
			Type:        typ.String(),
		}
		err = f.repo.InsertStationExtendedDataMapping(f.db.Ctx, mappingInput)
		if err != nil {
			return err
		}
	}
	return nil
}
