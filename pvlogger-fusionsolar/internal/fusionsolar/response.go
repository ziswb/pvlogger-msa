package fusionsolar

import (
	"fmt"
	"net/http"
)

// maxErrorBodyBytes caps how much of an unexpected reply ends up in the error.
const maxErrorBodyBytes = 512

// requireJSON200 fails when the generated client did not decode a 200 JSON
// reply (JSON200 is nil). Without this check the save step panics on the nil
// JSON200, and the reply that caused it is lost.
func requireJSON200(endpoint string, decoded bool, httpResp *http.Response, body []byte) error {
	if decoded {
		return nil
	}
	status, contentType := "no HTTP response", ""
	if httpResp != nil {
		status = httpResp.Status
		contentType = httpResp.Header.Get("Content-Type")
	}
	snippet := body
	suffix := ""
	if len(snippet) > maxErrorBodyBytes {
		snippet = snippet[:maxErrorBodyBytes]
		suffix = fmt.Sprintf(" (truncated, %d bytes total)", len(body))
	}
	return fmt.Errorf("%s: unexpected FusionSolar reply: status %q, content type %q, body %q%s",
		endpoint, status, contentType, snippet, suffix)
}

// APIError is a reply FusionSolar decoded fine but refused: success is not
// true. Such replies come back with HTTP 200 and carry no usable params or
// data (e.g. failCode 305 USER_MUST_RELOGIN, 401 "Invalid access to current
// interface!" when the API account has expired), so saving them would panic.
type APIError struct {
	FailCode int
	Message  string
}

func (e *APIError) Error() string {
	return fmt.Sprintf("FusionSolar refused the request: failCode %d, message %q", e.FailCode, e.Message)
}

// requireSuccess returns an *APIError unless the reply says success: true.
// A missing success field counts as a refusal.
func requireSuccess(resp FsResponseData) error {
	if s := resp.GetSuccess(); s != nil && *s {
		return nil
	}
	msg := ""
	if m := resp.GetMessage(); m != nil {
		msg = *m
	}
	return &APIError{FailCode: resp.GetFailCode(), Message: msg}
}
