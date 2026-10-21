package database

import (
	"context"
	"embed"
	"fmt"
	"io/fs"

	"pv-logger/logging"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/tern/v2/migrate"
)

// VersionTable holds tern's migration history. Deliberately not `schema_version`
// or anything Flyway-like: the Kotlin era left a `flyway_schema_history` table in
// fsdata that nothing reads any more (backlog item 29).
const VersionTable = "public.tern_schema_version"

//go:embed migrations/*.sql
var migrationFiles embed.FS

// Migrate applies all pending fsdata migrations. tern takes an advisory lock, so
// concurrent runs wait for each other. It returns the version before and after.
func Migrate(ctx context.Context, conn *pgx.Conn) (from, to int32, err error) {
	migrator, err := migrate.NewMigrator(ctx, conn, VersionTable)
	if err != nil {
		return 0, 0, fmt.Errorf("create migrator: %w", err)
	}
	files, err := fs.Sub(migrationFiles, "migrations")
	if err != nil {
		return 0, 0, fmt.Errorf("open embedded migrations: %w", err)
	}
	if err := migrator.LoadMigrations(files); err != nil {
		return 0, 0, fmt.Errorf("load migrations: %w", err)
	}
	from, err = migrator.GetCurrentVersion(ctx)
	if err != nil {
		return 0, 0, fmt.Errorf("read current version: %w", err)
	}
	migrator.OnStart = func(sequence int32, name, direction, _ string) {
		logging.Info("applying migration", "sequence", sequence, "name", name, "direction", direction)
	}
	if err := migrator.Migrate(ctx); err != nil {
		return from, 0, fmt.Errorf("migrate: %w", err)
	}
	to, err = migrator.GetCurrentVersion(ctx)
	if err != nil {
		return from, 0, fmt.Errorf("read new version: %w", err)
	}
	return from, to, nil
}
