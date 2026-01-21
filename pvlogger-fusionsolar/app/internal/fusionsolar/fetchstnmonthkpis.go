package fusionsolar

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"pvlogger/internal/config"
	"pvlogger/internal/model"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
)

func (f *FusionSolarAccess) FetchMonthData() (*model.KpiStationMonthResponse, error) {
	log.Default().Println("Fetching month data...")
	client, err := model.NewClientWithResponses(config.FsUrl(), WithBearerToken(f.token))
	if err != nil {
		return nil, err
	}
	kpiStationMonthJSONRequestBody := model.KpiStationMonthJSONRequestBody{StationCodes: config.FsConfig().StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationMonthWithResponse(context.Background(), kpiStationMonthJSONRequestBody)
	if err != nil {
		return nil, err
	}
	log.Default().Println("Station Month KPIs fetched successfully.")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationMonthKpiResponse(response *model.KpiStationMonthResponse) error {
	log.Default().Println("Saving Station Month KPI Response to Database...")
	responseId, err := f.insertStationMonthKpiResponseParams(response)
	if err != nil {
		return err
	}
	err = f.insertStationMonthKpiResponseBasicParams(responseId, response) // FsResponseParamsExtended.save
	if err != nil {
		return err
	}
	return f.upsertStationMonthKpi(responseId, response) // saveExtendedData
}

func (f *FusionSolarAccess) insertStationMonthKpiResponseParams(response *model.KpiStationMonthResponse) (int32, error) {
	log.Default().Println("Saving Station Month Kpi Response Params to database...")
	raw, err := json.RawMessage.MarshalJSON(response.Body)
	if err != nil {
		return 0, err
	}
	failCode := int32(response.JSON200.FailCode)
	message := messageAsPgTextOrNil(response.JSON200.Message)
	success := boolOrFalse(response.JSON200.Success)
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	i, err := f.insertFsResponseParams(failCode, message, raw, success, timestamp)
	log.Default().Println("Station Month Kpi response saved successfully.")
	return i, err
}

func (f *FusionSolarAccess) insertStationMonthKpiResponseBasicParams(responseId int32, response *model.KpiStationMonthResponse) error {
	log.Default().Println("Saving Station Month Kpi Basic Params  to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	stationCodes := pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes}
	currentTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime}
	collectTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CollectTime}
	err := f.insertFsResponseParamsExtended(responseId, stationCodes, currentTime, collectTime)
	log.Default().Println("Station Month Kpi response saved successfully.")
	return err
}

func (f *FusionSolarAccess) upsertStationMonthKpi(responseId int32, response *model.KpiStationMonthResponse) error {
	log.Default().Println("Saving Station Month Kpi to database...")
	data := *response.JSON200.Data
	err := f.upsertExtendedKpis(responseId, model.StationMonthKpi, data)
	if err != nil {
		return err
	}
	return nil
}
