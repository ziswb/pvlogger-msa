package database

import (
	"context"
	"fmt"
	"strings"

	"pv-logger/logging"
	"pv-logger/retry"

	"github.com/jackc/pgx/v5"
)

type Database struct {
	Conn *pgx.Conn
	Ctx  context.Context
}

// NewDatabase connects, retrying a refused connect (retry.Refused): kube-router
// admits a new pod's IP to the namespace's podSelector policies only a moment
// after the pod starts, and a CronJob connects right away (backlog 59).
func NewDatabase(host string, port int, dbname string, user string, password string, sslmode string) (*Database, error) {
	ctx := context.Background()
	cfg, err := connConfig(host, port, dbname, user, password, sslmode)
	if err != nil {
		return nil, err
	}
	conn, err := retry.OnRefused(ctx, retry.Refused, "database connect", func(ctx context.Context) (*pgx.Conn, error) {
		return pgx.ConnectConfig(ctx, cfg)
	}, "host", host)
	if err != nil {
		return nil, err
	}
	return &Database{
		Conn: conn,
		Ctx:  ctx,
	}, nil
}

// connConfig builds the connection settings. The values are quoted, so a
// space, quote or backslash in one cannot break the connection string, and the
// password is set on the parsed config instead of entering the string at all.
// connect_timeout: an unreachable server fails the run in seconds instead of
// hanging on the TCP connect (backlog 45). Each run opens its own connection,
// so stale pooled connections, the data accessor's problem, cannot happen here.
func connConfig(host string, port int, dbname string, user string, password string, sslmode string) (*pgx.ConnConfig, error) {
	cfg, err := pgx.ParseConfig(fmt.Sprintf("host=%s port=%d dbname=%s user=%s sslmode=%s connect_timeout=10",
		quote(host), port, quote(dbname), quote(user), quote(sslmode)))
	if err != nil {
		return nil, fmt.Errorf("database settings: %w", err)
	}
	cfg.Password = password
	return cfg, nil
}

// quote makes v a single-quoted keyword/value connection-string value.
func quote(v string) string {
	return "'" + strings.NewReplacer(`\`, `\\`, `'`, `\'`).Replace(v) + "'"
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
