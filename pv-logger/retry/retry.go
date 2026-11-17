// Package retry retries a connection attempt that the other side refused.
//
// A refused connection never reached the server, so retrying it is safe for
// every kind of call. In the cluster it is also expected for a moment: a new
// pod reaches in-namespace servers only once kube-router has admitted its IP
// to the podSelector policies, and a CronJob connects right away (backlog 57,
// 59). Every other error is returned at once.
package retry

import (
	"context"
	"errors"
	"math/rand/v2"
	"syscall"
	"time"

	"pv-logger/logging"
)

// Policy bounds the retries: up to Attempts calls, the wait between them
// doubling from Initial up to Max, each wait jittered to between half and all
// of it, so pods started together do not retry in lockstep.
type Policy struct {
	Attempts int
	Initial  time.Duration
	Max      time.Duration
}

// Refused is the policy of the in-cluster calls: 10 attempts with at most
// 0.2+0.4+0.8+1.6+6×2 = 15 s of waiting, enough for kube-router. Meant for a
// call made once per run; the Modbus connect, repeated on every request retry,
// has a shorter policy of its own (backlog 62).
var Refused = Policy{Attempts: 10, Initial: 200 * time.Millisecond, Max: 2 * time.Second}

// IsRefused reports whether err is a refused connection.
func IsRefused(err error) bool {
	return errors.Is(err, syscall.ECONNREFUSED)
}

// OnRefused calls fn until it succeeds, fails other than with a refused
// connection, the attempts run out or ctx ends. what and attrs name the call in
// the warning logged before each retry.
func OnRefused[T any](ctx context.Context, p Policy, what string, fn func(ctx context.Context) (T, error), attrs ...any) (T, error) {
	wait := p.Initial
	for attempt := 1; ; attempt++ {
		v, err := fn(ctx)
		if err == nil || !IsRefused(err) || attempt >= p.Attempts {
			return v, err
		}
		// attrs[:len:len] makes append copy instead of writing into spare
		// capacity of the caller's slice.
		logging.Warn(what+": connection refused, retrying", append(attrs[:len(attrs):len(attrs)], "attempt", attempt)...)
		timer := time.NewTimer(jitter(wait))
		select {
		case <-ctx.Done():
			timer.Stop()
			var zero T
			return zero, ctx.Err()
		case <-timer.C:
		}
		wait = min(2*wait, p.Max)
	}
}

func jitter(d time.Duration) time.Duration {
	if d <= 1 {
		return d
	}
	half := d / 2
	return half + rand.N(d-half+1)
}
