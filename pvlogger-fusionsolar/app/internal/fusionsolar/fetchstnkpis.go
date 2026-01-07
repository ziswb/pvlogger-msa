package fusionsolar

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
	"time"

	"github.com/jackc/pgx/v5/pgtype"
)

func (f FusionSolarAccess) FetchStationRealTimeData() error {
	log.Default().Println("Fetching station real time data...")
	client, err := model.NewClientWithResponses(config.FsUrl(), addTokenClientOption(f.token))
	if err != nil {
		return err
	}
	stationRealKpiJSONRequestBody := model.StationRealKpiJSONRequestBody{StationCodes: config.FsConfig().StationCode}
	response, err := client.StationRealKpiWithResponse(context.TODO(), stationRealKpiJSONRequestBody)
	if err != nil {
		return err
	}
	log.Default().Println("Station real time data fetched successfully.")
	f.StationKpiResponse = response
	return nil
}

func (f FusionSolarAccess) SaveStationRealKpiResponse() error {
	log.Default().Println("Saving response to database...")
	responseId, err := f.insertStationRealKpiResponseParams()
	if err != nil {
		return err
	}
	err = f.insertStationRealKpiResponseBasicParams(responseId)
	if err != nil {
		return err
	}
	err = f.insertStationRealKpi(responseId)
	if err != nil {
		return err
	}
	log.Default().Println("Response saved successfully.")
	return nil
}

func (f FusionSolarAccess) insertStationRealKpiResponseParams() (int32, error) {
	log.Default().Println("Saving Station Real Kpi response to database...")
	raw, err := json.RawMessage.MarshalJSON(f.StationKpiResponse.Body)
	if err != nil {
		return 0, err
	}
	failCode := int32(f.StationKpiResponse.JSON200.FailCode)
	message := messageAsPgTextStnKpi(f.StationKpiResponse)
	success := boolOrFalseStnKpi(f.StationKpiResponse)
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	i, err := f.insertFsResponseParams(failCode, message, raw, success, timestamp)
	log.Default().Println("Station Real Kpi response saved successfully.")
	return i, err
}

func (f FusionSolarAccess) insertStationRealKpiResponseBasicParams(responseId int32) error {
	log.Default().Println("Saving Station Real Kpi response params to database...")
	if f.StationKpiResponse.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	currentTime := pgtype.Int8{Valid: true, Int64: *f.StationKpiResponse.JSON200.Params.CurrentTime}
	stationCodes := pgtype.Text{Valid: true, String: *f.StationKpiResponse.JSON200.Params.StationCodes}
	err := f.insertFsResponseParamsBasicParams(currentTime, responseId, stationCodes)
	log.Default().Println("Station Real Kpi response params saved successfully.")
	return err
}

func (f FusionSolarAccess) insertFsResponseParamsBasicParams(currentTime pgtype.Int8, responseId int32, stationCodes pgtype.Text) error {
	log.Default().Println("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsBasicParams{
		CurrentTime:  currentTime,
		Response:     responseId,
		StationCodes: stationCodes,
	}
	err := database.New(f.Dao.Database.Conn).InsertFsResponseParamsBasic(f.Dao.Database.Ctx, responseParamsBasic)
	log.Default().Println("Response params saved successfully.")
	return err
}

func (f FusionSolarAccess) insertStationRealKpi(responseId int32) error {
	log.Default().Println("Saving station real kpi to database...")
	data := *f.StationKpiResponse.JSON200.Data
	for i := 0; i < len(data); i++ {
		record := database.InsertFsStationRealKpiParams{
			DayIncome:       float64(data[i].DataItemMap.DayIncome),
			DayPower:        float64(data[i].DataItemMap.DayPower),
			MonthPower:      float64(data[i].DataItemMap.MonthPower),
			RealHealthState: int32(data[i].DataItemMap.RealHealthState),
			Response:        responseId,
			StationCode:     pgtype.Text{Valid: true, String: data[i].StationCode},
			TotalIncome:     float64(data[i].DataItemMap.TotalIncome),
			TotalPower:      float64(data[i].DataItemMap.TotalPower),
			Type:            model.STATION_REAL_KPI.String(),
		}
		err := database.New(f.Dao.Database.Conn).InsertFsStationRealKpi(f.Dao.Database.Ctx, record)
		if err != nil {
			return err
		}
	}
	log.Default().Println("Station real kpi saved successfully.")
	return nil
}
