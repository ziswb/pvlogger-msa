// Command mock-server serves the synthetic plant over Modbus TCP in place of
// the SmartDongle, for local development only (ADR-0025).
//
// Environment: MOCK_PORT (default 8502; the chart's Service maps 502 to it),
// DONGLE_DEVICE_ID (default 100, as for the collectors), TZ (the plant's
// local time: day curve and daily-yield reset).
package main

import (
	"context"
	"fmt"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"
	// The distroless image has no zoneinfo; TZ needs the embedded database.
	_ "time/tzdata"

	"pv-logger/config"
	"pv-logger/logging"
	sharedconfig "pvlogger-shared-modbus/config"

	"pvlogger-modbus-mock/internal/mock"
	"pvlogger-modbus-mock/internal/plant"
)

func main() {
	port := config.GetIntEnv("MOCK_PORT", "8502")
	dongleUnit := sharedconfig.DevIdConfig().DongleDeviceId

	p := plant.New(plant.Config{Location: time.Local})
	// The first snapshot sums every day since the plant's origin. Do it before
	// listening, so the readiness probe passes only once requests are fast.
	start := time.Now()
	p.At(start)
	logging.Info("plant counters computed", "duration", time.Since(start).String())
	ln, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logging.Fatal("cannot listen", "port", port, "error", err)
	}
	logging.Info("serving the synthetic plant over Modbus TCP",
		"port", port, "dongle_unit", dongleUnit, "time_zone", time.Local.String())
	// Pod deletion sends SIGTERM: stop accepting and exit at once instead of
	// waiting for the grace period's SIGKILL.
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, os.Interrupt)
	defer stop()
	go func() {
		<-ctx.Done()
		_ = ln.Close()
	}()
	if err := mock.NewServer(p, dongleUnit, time.Now).Serve(ln); err != nil {
		logging.Fatal("mock server stopped", "error", err)
	}
	logging.Info("mock server shut down")
}
