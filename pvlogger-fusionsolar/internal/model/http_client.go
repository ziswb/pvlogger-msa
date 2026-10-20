package model

import (
	"net/http"
	"time"
)

// RequestTimeout bounds every FusionSolar API call, reading the body included.
// The generated NewClient falls back to an http.Client without a timeout, so a
// stalled connection would hang the CronJob (security finding 41). 30 s, like
// the other Go HTTP clients in the repo. A variable only so tests can shorten it.
var RequestTimeout = 30 * time.Second

// WithTimeout gives the generated client an http.Client bounded by RequestTimeout.
// Every hand-written NewClient / NewClientWithResponses call passes it.
func WithTimeout() ClientOption {
	return WithHTTPClient(&http.Client{Timeout: RequestTimeout})
}
