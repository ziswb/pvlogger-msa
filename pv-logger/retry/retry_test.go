package retry

import (
	"context"
	"errors"
	"fmt"
	"syscall"
	"testing"
	"time"
)

var fast = Policy{Attempts: 4, Initial: time.Millisecond, Max: 2 * time.Millisecond}

var errRefused = fmt.Errorf("dial tcp: %w", syscall.ECONNREFUSED)

// calls returns fn failing with the given errors in turn, then succeeding.
func calls(errs ...error) (func(context.Context) (int, error), *int) {
	n := 0
	return func(context.Context) (int, error) {
		n++
		if n <= len(errs) {
			return 0, errs[n-1]
		}
		return 42, nil
	}, &n
}

func TestOnRefused_RetriesUntilSuccess(t *testing.T) {
	fn, n := calls(errRefused, errRefused)
	v, err := OnRefused(context.Background(), fast, "test", fn)
	if err != nil || v != 42 || *n != 3 {
		t.Fatalf("got %d, %v after %d calls; want 42, nil after 3", v, err, *n)
	}
}

func TestOnRefused_OtherErrorsAreNotRetried(t *testing.T) {
	boom := errors.New("boom")
	fn, n := calls(boom)
	if _, err := OnRefused(context.Background(), fast, "test", fn); !errors.Is(err, boom) || *n != 1 {
		t.Fatalf("got %v after %d calls; want boom after 1", err, *n)
	}
}

func TestOnRefused_GivesUpAfterAttempts(t *testing.T) {
	fn, n := calls(errRefused, errRefused, errRefused, errRefused, errRefused)
	if _, err := OnRefused(context.Background(), fast, "test", fn); !IsRefused(err) || *n != fast.Attempts {
		t.Fatalf("got %v after %d calls; want errRefused after %d", err, *n, fast.Attempts)
	}
}

func TestOnRefused_StopsWhenContextEnds(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	slow := Policy{Attempts: 10, Initial: time.Hour, Max: time.Hour}
	fn := func(context.Context) (int, error) { cancel(); return 0, errRefused }
	start := time.Now()
	if _, err := OnRefused(ctx, slow, "test", fn); !errors.Is(err, context.Canceled) {
		t.Fatalf("got %v, want context.Canceled", err)
	}
	if time.Since(start) > time.Second {
		t.Fatal("waited despite the cancelled context")
	}
}

func TestJitter_StaysBetweenHalfAndAll(t *testing.T) {
	for range 1000 {
		if j := jitter(100 * time.Millisecond); j < 50*time.Millisecond || j > 100*time.Millisecond {
			t.Fatalf("jitter(100ms) = %v", j)
		}
	}
}

func TestRefused_TotalWaitIsBounded(t *testing.T) {
	total, wait := time.Duration(0), Refused.Initial
	for range Refused.Attempts - 1 {
		total += wait
		wait = min(2*wait, Refused.Max)
	}
	if total > 15*time.Second {
		t.Fatalf("worst-case wait %v, documented as at most 15 s", total)
	}
}
