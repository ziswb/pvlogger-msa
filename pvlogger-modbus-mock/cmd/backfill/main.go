// Command backfill writes the synthetic plant's last days as history into the
// data accessor, for local development only (ADR-0025). The data accessor must
// be a dev build (image build arg DEV_ACCEPT_CLIENT_TIMESTAMPS=true, as Tilt
// builds it; any other build refuses the dated raw rows); only minutes without a KPI row are filled, so a rerun completes an
// interrupted run and is otherwise harmless.
//
// Environment: BACKFILL_DAYS (default 30, counted back from today's local
// midnight), DA_HOST/DA_PORT/DA_PROTOCOL (the data accessor, as for the
// collectors), TZ (the plant's local time, as for the mock server).
package main

import (
	"context"
	"errors"
	"os"
	"os/signal"
	"syscall"
	"time"
	// The distroless image has no zoneinfo; TZ needs the embedded database.
	_ "time/tzdata"

	"pv-logger/config"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"
	"pvlogger-shared-modbus/persistence"

	"pvlogger-modbus-mock/internal/backfill"
	"pvlogger-modbus-mock/internal/plant"
)

func main() {
	days := config.GetIntEnv("BACKFILL_DAYS", "30")
	da := sharedconfig.DataAccessorCfg()
	client := persistence.NewPersistingService(da.Host, da.Port, da.Protocol, sharedconfig.DataAccessorBasePath)

	until := time.Now()
	today := time.Date(until.Year(), until.Month(), until.Day(), 0, 0, 0, 0, time.Local)
	from := today.AddDate(0, 0, -days)
	logging.Info("backfilling the synthetic plant", "from", from, "until", until, "time_zone", time.Local.String())
	// Pod deletion sends SIGTERM: stop between two minutes, a rerun completes it.
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
	defer stop()
	err := backfill.Run(ctx, plant.New(plant.Config{Location: time.Local}), client, from, until)
	if errors.Is(err, context.Canceled) {
		logging.Warn("backfill interrupted; a rerun completes it", "duration", time.Since(until).Round(time.Second))
		stop()
		os.Exit(1)
	}
	if err != nil {
		logging.Fatal("backfill failed", "error", err)
	}
	logging.Info("backfill finished", "duration", time.Since(until).Round(time.Second))
}
