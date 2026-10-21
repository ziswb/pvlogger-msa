package jobs

import (
	"context"

	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/database"

	"pv-logger/logging"
)

// Migrate is the job name that applies the fsdata schema migrations. It runs
// manually as a plain K8s Job, never on a schedule (backlog item 29).
const Migrate = "MIGRATE"

func RunMigrate(cfg *config.Config) {
	dbCfg := cfg.Database
	db, err := database.NewDatabase(dbCfg.Host, dbCfg.Port, dbCfg.DbName, dbCfg.Username, dbCfg.Password, dbCfg.SslMode)
	if err != nil {
		logging.Fatal("failed to connect to database", "error", err)
	}
	defer func() { _ = db.Close() }()

	from, to, err := database.Migrate(context.Background(), db.Conn)
	if err != nil {
		_ = db.Close()
		logging.Fatal("migration failed", "from", from, "error", err)
	}
	if from == to {
		logging.Info("schema up to date", "version", to)
		return
	}
	logging.Info("schema migrated", "from", from, "to", to)
}
