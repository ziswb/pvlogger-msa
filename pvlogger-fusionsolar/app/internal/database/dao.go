package database

import (
	"pvlogger-go-shared/logging"
)

type Dao struct {
	Database Database
}

func (db Database) CloseDb() {
	err := db.Conn.Close(db.Ctx)
	if err != nil {
		logging.Error("failed to close database connection", "error", err)
	}
}
