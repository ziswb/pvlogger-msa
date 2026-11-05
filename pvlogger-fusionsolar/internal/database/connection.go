package database

import (
	"context"
	"fmt"

	"pv-logger/logging"

	"github.com/jackc/pgx/v5"
)

type Database struct {
	Conn *pgx.Conn
	Ctx  context.Context
}

func NewDatabase(host string, port int, dbname string, user string, password string, sslmode string) (*Database, error) {
	ctx := context.Background()
	// connect_timeout: an unreachable server fails the run in seconds instead of
	// hanging on the TCP connect (backlog 45). Each run opens its own connection,
	// so stale pooled connections, the data accessor's problem, cannot happen here.
	psqlInfo := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s sslmode=%s connect_timeout=10", host, port, dbname, user, password, sslmode)
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
