package auth

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"pvlogger/internal/config"
	"pvlogger/internal/model"

	"pvlogger-go-shared/logging"
)

type Token struct {
	Token string
}

func Authenticate() (token *Token, err error) {
	logging.Info("authenticating")
	client, err := model.NewClient(config.FsUrl())
	if err != nil {
		return nil, err
	}
	jsonLoginRequestBody := model.LoginJSONRequestBody{SystemCode: config.FsConfig().Password, UserName: config.FsConfig().Username}
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
