package database

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"

	"pvlogger-battery-simulator/internal/sim"
)

type Database struct {
	Conn *pgx.Conn
	Ctx  context.Context
}

func NewDatabase(host string, port int, dbname string, user string, password string, sslmode string) (*Database, error) {
	ctx := context.Background()
	psqlInfo := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s sslmode=%s", host, port, dbname, user, password, sslmode)
	conn, err := pgx.Connect(ctx, psqlInfo)
	if err != nil {
		return nil, err
	}
	return &Database{Conn: conn, Ctx: ctx}, nil
}

func (db *Database) Close() error {
	if db == nil || db.Conn == nil {
		return nil
	}
	return db.Conn.Close(db.Ctx)
}

// StreamSamples reads KPI rows in [from, to) in chronological order and
// passes each one to handle.
func (db *Database) StreamSamples(from, to time.Time, handle func(sim.Sample)) (int, error) {
	rows, err := db.Conn.Query(db.Ctx, `
		SELECT date_created, solar_power, solar_to_house_power, grid_to_house_power, solar_to_grid_power
		FROM house_power_kpi_values
		WHERE date_created >= $1 AND date_created < $2
		ORDER BY date_created`, from, to)
	if err != nil {
		return 0, err
	}
	defer rows.Close()

	count := 0
	for rows.Next() {
		var s sim.Sample
		if err := rows.Scan(&s.Time, &s.SolarKW, &s.SolarToHouseKW, &s.GridToHouseKW, &s.SolarToGridKW); err != nil {
			return count, err
		}
		handle(s)
		count++
	}
	return count, rows.Err()
}
