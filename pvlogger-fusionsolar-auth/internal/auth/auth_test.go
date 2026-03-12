package auth

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"pvlogger-fusionsolar-auth/internal/config"
	"strings"
	"testing"
)

// --- cookieValue tests ---

func TestCookieValue_Found(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "session", Value: "abc"},
		{Name: "XSRF-TOKEN", Value: "tok123"},
	}
	if got := cookieValue(cookies, "XSRF-TOKEN"); got != "tok123" {
		t.Errorf("expected 'tok123', got '%s'", got)
	}
}

func TestCookieValue_NotFound(t *testing.T) {
	cookies := []*http.Cookie{{Name: "session", Value: "abc"}}
	if got := cookieValue(cookies, "XSRF-TOKEN"); got != "" {
		t.Errorf("expected empty string, got '%s'", got)
	}
}

func TestCookieValue_EmptyCookies(t *testing.T) {
	if got := cookieValue([]*http.Cookie{}, "XSRF-TOKEN"); got != "" {
		t.Errorf("expected empty string, got '%s'", got)
	}
}

func TestCookieValue_NilCookies(t *testing.T) {
	if got := cookieValue(nil, "XSRF-TOKEN"); got != "" {
		t.Errorf("expected empty string, got '%s'", got)
	}
}

func TestCookieValue_FirstMatchReturned(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "tok", Value: "first"},
		{Name: "tok", Value: "second"},
	}
	if got := cookieValue(cookies, "tok"); got != "first" {
		t.Errorf("expected 'first', got '%s'", got)
	}
}

// --- jsonReaderType tests ---

func TestJsonReader_ReadAll(t *testing.T) {
	data := []byte("hello world")
	r := jsonReader(data)
	result, err := io.ReadAll(r)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if string(result) != "hello world" {
		t.Errorf("expected 'hello world', got '%s'", string(result))
	}
}

func TestJsonReader_Empty(t *testing.T) {
	r := jsonReader([]byte{})
	n, err := r.Read(make([]byte, 10))
	if n != 0 {
		t.Errorf("expected 0 bytes, got %d", n)
	}
	if err != io.EOF {
		t.Errorf("expected io.EOF, got %v", err)
	}
}

func TestJsonReader_SmallBuffer(t *testing.T) {
	data := []byte("abcdef")
	r := jsonReader(data)

	buf := make([]byte, 3)
	n, err := r.Read(buf)
	if n != 3 || err != nil {
		t.Fatalf("first read: n=%d, err=%v", n, err)
	}
	if string(buf[:n]) != "abc" {
		t.Errorf("first read: expected 'abc', got '%s'", string(buf[:n]))
	}

	n, err = r.Read(buf)
	if n != 3 || err != nil {
		t.Fatalf("second read: n=%d, err=%v", n, err)
	}
	if string(buf[:n]) != "def" {
		t.Errorf("second read: expected 'def', got '%s'", string(buf[:n]))
	}

	n, err = r.Read(buf)
	if n != 0 || err != io.EOF {
		t.Errorf("third read: expected (0, EOF), got (%d, %v)", n, err)
	}
}

func TestJsonReader_SingleByte(t *testing.T) {
	data := []byte("X")
	r := jsonReader(data)
	result, err := io.ReadAll(r)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if string(result) != "X" {
		t.Errorf("expected 'X', got '%s'", string(result))
	}
}

// --- Authenticate tests ---

func newTestConfig(serverURL string) *config.Config {
	return &config.Config{
		FusionSolar: config.FusionSolarConfig{
			Username: "testuser",
			Password: "testpass",
			Protocol: "http",
			Host:     strings.TrimPrefix(serverURL, "http://"),
			Path:     "",
		},
	}
}

func TestAuthenticate_Success(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("expected POST, got %s", r.Method)
		}
		if r.URL.Path != "/login" {
			t.Errorf("expected /login path, got %s", r.URL.Path)
		}
		if ct := r.Header.Get("Content-Type"); ct != "application/json" {
			t.Errorf("expected Content-Type application/json, got %s", ct)
		}

		var loginReq LoginRequest
		body, _ := io.ReadAll(r.Body)
		if err := json.Unmarshal(body, &loginReq); err != nil {
			t.Fatalf("failed to parse request body: %v", err)
		}
		if loginReq.UserName != "testuser" {
			t.Errorf("expected username 'testuser', got '%s'", loginReq.UserName)
		}
		if loginReq.SystemCode != "testpass" {
			t.Errorf("expected systemCode 'testpass', got '%s'", loginReq.SystemCode)
		}

		http.SetCookie(w, &http.Cookie{Name: "XSRF-TOKEN", Value: "my-token-123"})
		w.Header().Set("Content-Type", "application/json")
		resp := LoginResponse{FailCode: 0, Success: true}
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	token, err := Authenticate(newTestConfig(server.URL))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if token != "my-token-123" {
		t.Errorf("expected 'my-token-123', got '%s'", token)
	}
}

func TestAuthenticate_NonZeroFailCode(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.SetCookie(w, &http.Cookie{Name: "XSRF-TOKEN", Value: "tok"})
		w.Header().Set("Content-Type", "application/json")
		resp := LoginResponse{FailCode: 305, Message: "invalid credentials"}
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	_, err := Authenticate(newTestConfig(server.URL))
	if err == nil {
		t.Fatal("expected error for non-zero FailCode")
	}
	if !strings.Contains(err.Error(), "invalid credentials") {
		t.Errorf("expected error to contain 'invalid credentials', got '%s'", err.Error())
	}
	if !strings.Contains(err.Error(), "305") {
		t.Errorf("expected error to contain fail code '305', got '%s'", err.Error())
	}
}

func TestAuthenticate_MissingCookie(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// No XSRF-TOKEN cookie set
		w.Header().Set("Content-Type", "application/json")
		resp := LoginResponse{FailCode: 0, Success: true}
		json.NewEncoder(w).Encode(resp)
	}))
	defer server.Close()

	_, err := Authenticate(newTestConfig(server.URL))
	if err == nil {
		t.Fatal("expected error for missing XSRF-TOKEN cookie")
	}
	if !strings.Contains(err.Error(), "XSRF-TOKEN") {
		t.Errorf("expected error about XSRF-TOKEN, got '%s'", err.Error())
	}
}

func TestAuthenticate_HttpError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	_, err := Authenticate(newTestConfig(server.URL))
	if err == nil {
		t.Fatal("expected error for HTTP 500")
	}
	if !strings.Contains(err.Error(), "500") {
		t.Errorf("expected error to contain '500', got '%s'", err.Error())
	}
}

func TestAuthenticate_InvalidJsonResponse(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte("not valid json"))
	}))
	defer server.Close()

	_, err := Authenticate(newTestConfig(server.URL))
	if err == nil {
		t.Fatal("expected error for invalid JSON response")
	}
	if !strings.Contains(err.Error(), "unmarshal") {
		t.Errorf("expected unmarshal error, got '%s'", err.Error())
	}
}

func TestAuthenticate_ServerUnreachable(t *testing.T) {
	cfg := newTestConfig("http://127.0.0.1:1") // port 1 is unlikely to be listening
	_, err := Authenticate(cfg)
	if err == nil {
		t.Fatal("expected error for unreachable server")
	}
	if !strings.Contains(err.Error(), "login request failed") {
		t.Errorf("expected 'login request failed' error, got '%s'", err.Error())
	}
}
