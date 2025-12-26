package fusionsolar

import (
	"context"
	"encoding/json"
	"errors"
	"log"
	"pvlogger/internal/auth"
	"pvlogger/internal/config"
	"pvlogger/internal/database"
	"pvlogger/internal/model"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
)

func FetchStationRealTimeData(token auth.Token) (*model.StationRealKpiResponse, error) {
	log.Default().Println("Fetching station real time data...")
	client, err := model.NewClientWithResponses(config.FsUrl(), addTokenClientOption(token))
	if err != nil {
		return nil, err
	}
	stationRealKpiJSONRequestBody := model.StationRealKpiJSONRequestBody{StationCodes: config.FsConfig().StationCode}
	response, err := client.StationRealKpiWithResponse(context.TODO(), stationRealKpiJSONRequestBody)
	if err != nil {
		return nil, err
	}
	log.Default().Println("Station real time data fetched successfully.")
	return response, nil
}

func SaveStationRealKpiResponse(response *model.StationRealKpiResponse) error {
	log.Default().Println("Saving response to database...")
	db, err := database.NewDatabase(config.DbConfig().Host, config.DbConfig().Port, config.DbConfig().DbName, config.DbConfig().Username, config.DbConfig().Password)
	if err != nil {
		return err
	}
	defer func(conn *pgx.Conn, ctx context.Context) {
		err := conn.Close(ctx)
		if err != nil {
			log.Fatal(err)
		}
	}(db.Conn, db.Ctx)
	responseId, err := insertStationRealKpiResponseParams(response, db)
	if err != nil {
		return err
	}
	err = insertStationRealKpiResponseBasicParams(response, responseId, db)
	if err != nil {
		return err
	}
	err = insertStationRealKpi(response, responseId, db)
	if err != nil {
		return err
	}
	log.Default().Println("Response saved successfully.")
	return nil
}

func insertStationRealKpiResponseParams(response *model.StationRealKpiResponse, db *database.Database) (int32, error) {
	log.Default().Println("Saving Station Real Kpi response to database...")
	raw, err := json.RawMessage.MarshalJSON(response.Body)
	if err != nil {
		return 0, err
	}
	failCode := int32(response.JSON200.FailCode)
	message := messageAsPgTextStnKpi(response)
	success := boolOrFalseStnKpi(response)
	timestamp := pgtype.Timestamp{Valid: true, Time: time.Now()}
	i, err := insertFsResponseParams(failCode, message, raw, success, timestamp, err, db)
	log.Default().Println("Station Real Kpi response saved successfully.")
	return i, err
}

func insertStationRealKpiResponseBasicParams(response *model.StationRealKpiResponse, responseId int32, db *database.Database) error {
	log.Default().Println("Saving Station Real Kpi response params to database...")
	if response.JSON200.Params == nil {
		return errors.New("'response.JSON200.Params' is nil")
	}
	currentTime := pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime}
	stationCodes := pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes}
	err := insertFsResponseParamsBasicParams(currentTime, responseId, stationCodes, db)
	log.Default().Println("Station Real Kpi response params saved successfully.")
	return err
}

func insertFsResponseParamsBasicParams(currentTime pgtype.Int8, responseId int32, stationCodes pgtype.Text, db *database.Database) error {
	log.Default().Println("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsBasicParams{
		CurrentTime:  currentTime,
		Response:     responseId,
		StationCodes: stationCodes,
	}
	err := database.New(db.Conn).InsertFsResponseParamsBasic(db.Ctx, responseParamsBasic)
	log.Default().Println("Response params saved successfully.")
	return err
}

func insertStationRealKpi(response *model.StationRealKpiResponse, responseId int32, db *database.Database) error {
	log.Default().Println("Saving station real kpi to database...")
	data := *response.JSON200.Data
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
		err := database.New(db.Conn).InsertFsStationRealKpi(db.Ctx, record)
		if err != nil {
			return err
		}
	}
	log.Default().Println("Station real kpi saved successfully.")
	return nil
}
