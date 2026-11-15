// Package httpclient is the HTTP client of the collectors' in-cluster calls
// (data accessor, masterdata server).
package httpclient

import (
	"errors"
	"net/http"
	"syscall"
	"time"

	"pv-logger/logging"
)

const (
	// refusedAttempts × refusedWait bounds how long a refused connection is
	// retried: kube-router admits a new pod's IP to the namespace's
	// podSelector policies only a moment after the pod starts (backlog 57).
	refusedAttempts = 10
	refusedWait     = 500 * time.Millisecond
)

// New returns a client bounded by timeout that retries a refused connection.
// A refusal means the request never reached the server, so retrying is safe
// for every method, POST included.
func New(timeout time.Duration) *http.Client {
	return &http.Client{
		Timeout:   timeout,
		Transport: &retryRefused{next: http.DefaultTransport, attempts: refusedAttempts, wait: refusedWait},
	}
}

type retryRefused struct {
	next     http.RoundTripper
	attempts int
	wait     time.Duration
}

func (t *retryRefused) RoundTrip(req *http.Request) (*http.Response, error) {
	for attempt := 1; ; attempt++ {
		resp, err := t.next.RoundTrip(req)
		if err == nil || !errors.Is(err, syscall.ECONNREFUSED) || attempt >= t.attempts {
			return resp, err
		}
		// The failed attempt may have consumed the body; a retry needs a fresh one.
		if req.Body != nil && req.Body != http.NoBody {
			if req.GetBody == nil {
				return resp, err
			}
			body, bodyErr := req.GetBody()
			if bodyErr != nil {
				return resp, err
			}
			req = req.Clone(req.Context())
			req.Body = body
		}
		logging.Warn("connection refused, retrying", "url", req.URL.String(), "attempt", attempt)
		select {
		case <-req.Context().Done():
			return nil, req.Context().Err()
		case <-time.After(t.wait):
		}
	}
}
