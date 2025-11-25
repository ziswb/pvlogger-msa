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

func SaveResponse(response *model.StationRealKpiResponse) error {
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
	responseId, err := insertFsResponseGeneric(response, db)
	if err != nil {
		return err
	}
	err = insertFsResponseParams(response, responseId, db)
	if err != nil {
		return err
	}
	err = insertFsStationRealKpi(response, responseId, db)
	if err != nil {
		return err
	}
	log.Default().Println("Response saved successfully.")
	return nil
}

func insertFsResponseGeneric(response *model.StationRealKpiResponse, db *database.Database) (int32, error) {
	log.Default().Println("Saving generic response to database...")
	raw, err := json.RawMessage.MarshalJSON(response.Body)
	if err != nil {
		return 0, err
	}
	responseParams := database.InsertFsResponseParams{
		FailCode:  int32(response.JSON200.FailCode),
		Message:   messageAsPgText(response),
		Raw:       raw,
		Success:   boolOrFalse(response),
		Timestamp: pgtype.Timestamp{Valid: true, Time: time.Now()},
	}
	insertedFsResponse, err := database.New(db.Conn).InsertFsResponse(db.Ctx, responseParams)
	if err != nil {
		return 0, err
	}
	log.Default().Println("Generic response saved successfully.")
	return insertedFsResponse.ID, nil
}

func insertFsResponseParams(response *model.StationRealKpiResponse, responseId int32, db *database.Database) error {
	log.Default().Println("Saving response params to database...")
	responseParamsBasic := database.InsertFsResponseParamsBasicParams{
		CurrentTime:  pgtype.Int8{Valid: true, Int64: *response.JSON200.Params.CurrentTime},
		Response:     responseId,
		StationCodes: pgtype.Text{Valid: true, String: *response.JSON200.Params.StationCodes},
	}
	err := database.New(db.Conn).InsertFsResponseParamsBasic(db.Ctx, responseParamsBasic)
	log.Default().Println("Response params saved successfully.")
	return err
}

func insertFsStationRealKpi(response *model.StationRealKpiResponse, responseId int32, db *database.Database) error {
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

func boolOrFalse(response *model.StationRealKpiResponse) bool {
	if response.JSON200 != nil && response.JSON200.Success != nil {
		return *response.JSON200.Success
	}
	return false
}

func messageAsPgText(response *model.StationRealKpiResponse) pgtype.Text {
	if response.JSON200 != nil && response.JSON200.Message != nil {
		return pgtype.Text{Valid: true, String: *response.JSON200.Message}
	}
	return pgtype.Text{Valid: false, String: ""}
}

func addTokenClientOption(token auth.Token) model.ClientOption {
	return model.WithRequestEditorFn(func(ctx context.Context, req *http.Request) error {
		log.Default().Println("Adding token to request...")
		req.Header.Set("XSRF-TOKEN", token.Token)
		return nil
	})
}
