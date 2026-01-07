package database

import (
	"log"
)

type Dao struct {
	Database Database
}

func (db Database) CloseDb() {
	err := db.Conn.Close(db.Ctx)
	if err != nil {
		log.Default().Println(err)
	}
}
