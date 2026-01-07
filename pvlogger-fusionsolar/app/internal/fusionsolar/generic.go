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

func (f FusionSolarAccess) insertFsResponseParams(failCode int32, message pgtype.Text, raw []byte, success bool, timestamp pgtype.Timestamp) (int32, error) {
	log.Default().Println("Saving fs response params to database...")
	responseParams := database.InsertFsResponseParams{
		FailCode:  failCode,
		Message:   message,
		Raw:       raw,
		Success:   success,
		Timestamp: timestamp,
	}
	insertedFsResponse, err := database.New(f.Dao.Database.Conn).InsertFsResponse(f.Dao.Database.Ctx, responseParams)
	if err != nil {
		return 0, err
	}
	log.Default().Println("Fs response params saved successfully.")
	return insertedFsResponse.ID, nil
}

func addTokenClientOption(token auth.Token) model.ClientOption {
	return model.WithRequestEditorFn(func(ctx context.Context, req *http.Request) error {
		log.Default().Println("Adding token to request...")
		req.Header.Set("XSRF-TOKEN", token.Token)
		return nil
	})
}

func boolOrFalseDevKpi(response *model.DeviceRealKpiResponse) bool {
	if response.JSON200 != nil && response.JSON200.Success != nil {
		return *response.JSON200.Success
	}
	return false
}

func messageAsPgTextDevKpi(response *model.DeviceRealKpiResponse) pgtype.Text {
	if response.JSON200 != nil && response.JSON200.Message != nil {
		return pgtype.Text{Valid: true, String: *response.JSON200.Message}
	}
	return pgtype.Text{Valid: false, String: ""}
}

func boolOrFalseStnKpi(response *model.StationRealKpiResponse) bool {
	if response.JSON200 != nil && response.JSON200.Success != nil {
		return *response.JSON200.Success
	}
	return false
}

func messageAsPgTextStnKpi(response *model.StationRealKpiResponse) pgtype.Text {
	if response.JSON200 != nil && response.JSON200.Message != nil {
		return pgtype.Text{Valid: true, String: *response.JSON200.Message}
	}
	return pgtype.Text{Valid: false, String: ""}
}
