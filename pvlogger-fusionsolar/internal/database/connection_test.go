package database

import (
	"context"
	"errors"
	"fmt"
	"net"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
)

// pgx must keep the refusal recognisable through its error wrapping, or the
// retry never triggers.
func TestPgxReportsARefusedConnectAsRefused(t *testing.T) {
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	port := ln.Addr().(*net.TCPAddr).Port
	_ = ln.Close()

	_, err = pgx.Connect(context.Background(),
		fmt.Sprintf("host=127.0.0.1 port=%d dbname=x user=x password=x sslmode=disable connect_timeout=2", port))
	if !isRefused(err) {
		t.Fatalf("error %v is not recognised as a refused connection", err)
	}
}

func TestARefusedConnectIsRetriedUntilItSucceeds(t *testing.T) {
	calls := 0
	v, err := retryRefused(context.Background(), 10, time.Millisecond, func() (int, error) {
		calls++
		if calls < 3 {
			return 0, refused()
		}
		return 42, nil
	})
	if err != nil || v != 42 || calls != 3 {
		t.Errorf("got %v, %v after %d calls; want 42, nil after 3", v, err, calls)
	}
}

func TestOtherErrorsAndTheLastAttemptAreNotRetried(t *testing.T) {
	other := errors.New("password authentication failed")
	calls := 0
	_, err := retryRefused(context.Background(), 10, time.Millisecond, func() (int, error) {
		calls++
		return 0, other
	})
	if !errors.Is(err, other) || calls != 1 {
		t.Errorf("other error: %v after %d calls; want it after 1", err, calls)
	}

	calls = 0
	_, err = retryRefused(context.Background(), 4, time.Millisecond, func() (int, error) {
		calls++
		return 0, refused()
	})
	if !isRefused(err) || calls != 4 {
		t.Errorf("refusals: %v after %d calls; want a refusal after 4", err, calls)
	}
}

func refused() error {
	_, err := net.DialTimeout("tcp", closedAddr(), time.Second)
	return err
}

func closedAddr() string {
	ln, _ := net.Listen("tcp", "127.0.0.1:0")
	addr := ln.Addr().String()
	_ = ln.Close()
	return addr
}
