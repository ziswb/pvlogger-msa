package database

import (
	"context"
	"fmt"

	"pvlogger-go-shared/logging"

	"github.com/jackc/pgx/v5"
)

type Database struct {
	Conn *pgx.Conn
	Ctx  context.Context
}

func NewDatabase(host string, port int, dbname string, user string, password string) (*Database, error) {
	ctx := context.Background()
	psqlInfo := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s sslmode=disable", host, port, dbname, user, password)
	conn, err := pgx.Connect(ctx, psqlInfo)
	if err != nil {
		return nil, err
	}
	return &Database{
		Conn: conn,
		Ctx:  ctx,
	}, nil
}

func (db *Database) Close() error {
	if db == nil || db.Conn == nil {
		return nil
	}
	err := db.Conn.Close(db.Ctx)
	if err != nil {
		logging.Error("failed to close database connection", "error", err)
		return err
	}
	return nil
}
