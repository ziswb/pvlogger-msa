package database

import (
	"context"
	"errors"
	"fmt"
	"syscall"
	"time"

	"pv-logger/logging"

	"github.com/jackc/pgx/v5"
)

const (
	// refusedAttempts × refusedWait bounds how long a refused connect is
	// retried: kube-router admits a new pod's IP to the namespace's
	// podSelector policies only a moment after the pod starts, and a CronJob
	// connects right away (pvlogger-msa backlog 59).
	refusedAttempts = 10
	refusedWait     = 500 * time.Millisecond
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
	conn, err := retryRefused(ctx, refusedAttempts, refusedWait, func() (*pgx.Conn, error) {
		return pgx.Connect(ctx, psqlInfo)
	})
	if err != nil {
		return nil, err
	}
	return &Database{
		Conn: conn,
		Ctx:  ctx,
	}, nil
}

// retryRefused calls connect until it succeeds, fails other than with a
// refused connection, or attempts run out.
func retryRefused[T any](ctx context.Context, attempts int, wait time.Duration, connect func() (T, error)) (T, error) {
	for attempt := 1; ; attempt++ {
		v, err := connect()
		if err == nil || !isRefused(err) || attempt >= attempts {
			return v, err
		}
		logging.Warn("database connection refused, retrying", "attempt", attempt)
		select {
		case <-ctx.Done():
			return v, ctx.Err()
		case <-time.After(wait):
		}
	}
}

func isRefused(err error) bool {
	return errors.Is(err, syscall.ECONNREFUSED)
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
