package auth

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"pvlogger-fusionsolar/internal/config"
	"pvlogger-fusionsolar/internal/model"

	"pv-logger/logging"
)

type Token struct {
	Token string
}

func Authenticate(cfg *config.Config) (token *Token, err error) {
	logging.Info("authenticating")
	client, err := model.NewClient(cfg.FsUrl(), model.WithTimeout())
	if err != nil {
		return nil, err
	}
	jsonLoginRequestBody := model.LoginJSONRequestBody{SystemCode: cfg.Fs.Password, UserName: cfg.Fs.Username}
	response, err := client.Login(context.Background(), jsonLoginRequestBody)
	if err != nil {
		return nil, err
	}
	tokenValue := CookieValue(response.Cookies(), "XSRF-TOKEN")
	r := model.LoginResponse{}
	responseBody, err := io.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(responseBody, &r)
	if err != nil {
		return nil, err
	}
	logging.Info("login successful")
	return &Token{Token: tokenValue}, nil
}

func CookieValue(cookies []*http.Cookie, name string) (value string) {
	for _, c := range cookies {
		if c.Name == name {
			return c.Value
		}
	}
	return ""
}
