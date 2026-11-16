// Package httpclient is the HTTP client of the collectors' in-cluster calls
// (data accessor, masterdata server).
package httpclient

import (
	"context"
	"net/http"
	"time"

	"pv-logger/retry"
)

// New returns a client bounded by timeout that retries a refused connection
// (retry.Refused). A refusal means the request never reached the server, so
// retrying is safe for every method, POST included.
func New(timeout time.Duration) *http.Client {
	return &http.Client{
		Timeout:   timeout,
		Transport: &retryRefused{next: http.DefaultTransport, policy: retry.Refused},
	}
}

type retryRefused struct {
	next   http.RoundTripper
	policy retry.Policy
}

func (t *retryRefused) RoundTrip(req *http.Request) (*http.Response, error) {
	hasBody := req.Body != nil && req.Body != http.NoBody
	// A body that cannot be produced again cannot be resent.
	if hasBody && req.GetBody == nil {
		return t.next.RoundTrip(req)
	}
	attempt := 0
	return retry.OnRefused(req.Context(), t.policy, "HTTP request", func(context.Context) (*http.Response, error) {
		attempt++
		if attempt == 1 || !hasBody {
			return t.next.RoundTrip(req)
		}
		// The failed attempt may have consumed the body; a retry needs a fresh one.
		body, err := req.GetBody()
		if err != nil {
			return nil, err
		}
		retried := req.Clone(req.Context())
		retried.Body = body
		return t.next.RoundTrip(retried)
	}, "url", req.URL.String())
}
