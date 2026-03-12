package auth

import (
	"net/http"
	"testing"
)

func TestCookieValue_Found(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "session", Value: "abc123"},
		{Name: "XSRF-TOKEN", Value: "token-value"},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "token-value" {
		t.Errorf("expected 'token-value', got '%s'", result)
	}
}

func TestCookieValue_NotFound(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "session", Value: "abc123"},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_EmptyCookies(t *testing.T) {
	result := CookieValue([]*http.Cookie{}, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_NilCookies(t *testing.T) {
	result := CookieValue(nil, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}

func TestCookieValue_FirstMatchReturned(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "token", Value: "first"},
		{Name: "token", Value: "second"},
	}
	result := CookieValue(cookies, "token")
	if result != "first" {
		t.Errorf("expected 'first', got '%s'", result)
	}
}

func TestCookieValue_EmptyValue(t *testing.T) {
	cookies := []*http.Cookie{
		{Name: "XSRF-TOKEN", Value: ""},
	}
	result := CookieValue(cookies, "XSRF-TOKEN")
	if result != "" {
		t.Errorf("expected empty string, got '%s'", result)
	}
}
