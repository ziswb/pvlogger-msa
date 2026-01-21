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

func (f *FusionSolarAccess) FetchYearData() (*model.KpiStationYearResponse, error) {
	log.Default().Println("Fetching year data...")
	client, err := model.NewClientWithResponses(config.FsUrl(), WithBearerToken(f.token))
	if err != nil {
		return nil, err
	}
	kpiStationYearJSONRequestBody := model.KpiStationYearJSONRequestBody{StationCodes: config.FsConfig().StationCode, CollectTime: time.Now().UnixMilli()}
	response, err := client.KpiStationYearWithResponse(context.Background(), kpiStationYearJSONRequestBody)
	if err != nil {
		return nil, err
	}
	log.Default().Println("Station Year KPIs fetched successfully.")
	return response, nil
}

func (f *FusionSolarAccess) SaveStationYearKpiResponse(response *model.KpiStationYearResponse) error {
	log.Default().Println("Saving Station Year KPI Response to Database...")
	responseId, err := f.insertStationYearKpiResponseParams(response) // safeGenericResponse
	if err != nil {
		return err
	}
	err = f.insertStationYearKpiResponseBasicParams(responseId, response) // FsResponseParamsExtended.save
	if err != nil {
		return err
	}
	data := *response.JSON200.Data
	return f.upsertExtendedKpis(responseId, model.StationYearKpi, data) // saveExtendedData
}

func (f *FusionSolarAccess) insertStationYearKpiResponseParams(response *model.KpiStationYearResponse) (int32, error) {
	log.Default().Println("Saving Station Year Kpi Response Params to database...")
	raw, err := json.RawMessage.MarshalJSON(response.Body)
	if err != nil {
		return 0, err
	}
	failCode := int32(response.JSON200.FailCode)
	message := messageAsPgTextOrNil(response.JSON200.Message)
	success := boolOrFalse(response.JSON200.Success)
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	i, err := f.insertFsResponseParams(failCode, message, raw, success, timestamp)
	log.Default().Println("Station Year Kpi response saved successfully.")
	return i, err
}

func (f *FusionSolarAccess) insertStationYearKpiResponseBasicParams(responseId int32, response *model.KpiStationYearResponse) error {
	log.Default().Println("Saving Station Year Kpi Basic Params  to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	stationCodes := pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes}
	currentTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime}
	collectTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CollectTime}
	err := f.insertFsResponseParamsExtended(responseId, stationCodes, currentTime, collectTime)
	log.Default().Println("Station Year Kpi response saved successfully.")
	return err
}
