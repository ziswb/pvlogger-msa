package database

import (
	"context"
	"fmt"
	"net"
	"testing"

	"pv-logger/retry"

	"github.com/jackc/pgx/v5"
)

// pgx must keep the refusal recognisable through its error wrapping, or the
// retry (pv-logger/retry, tested there) never triggers.
func TestPgxReportsARefusedConnectAsRefused(t *testing.T) {
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	port := ln.Addr().(*net.TCPAddr).Port
	_ = ln.Close()

	_, err = pgx.Connect(context.Background(),
		fmt.Sprintf("host=127.0.0.1 port=%d dbname=x user=x password=x sslmode=disable connect_timeout=2", port))
	if !retry.IsRefused(err) {
		t.Fatalf("error %v is not recognised as a refused connection", err)
	}
}

// Values with spaces, quotes or backslashes used to break the Sprintf-built
// connection string (backlog 60).
func TestConnConfigKeepsAwkwardValuesIntact(t *testing.T) {
	for _, password := range []string{"plain", "with space", `it's`, `back\slash`, `a=b c='d'`, ""} {
		cfg, err := connConfig("db.example.invalid", 5433, "fs data", `us'er`, password, "disable")
		if err != nil {
			t.Fatalf("password %q: %v", password, err)
		}
		if cfg.Password != password || cfg.Host != "db.example.invalid" || cfg.Port != 5433 ||
			cfg.Database != "fs data" || cfg.User != `us'er` {
			t.Errorf("password %q: got host %q port %d db %q user %q password %q",
				password, cfg.Host, cfg.Port, cfg.Database, cfg.User, cfg.Password)
		}
		if cfg.ConnectTimeout == 0 {
			t.Errorf("password %q: no connect timeout", password)
		}
	}
}

func TestConnConfigRejectsAnUnknownSslMode(t *testing.T) {
	if _, err := connConfig("h", 5432, "d", "u", "p", "sometimes"); err == nil {
		t.Fatal("an invalid sslmode was accepted")
	}
}
