package fusionsolar

import (
	"context"
	"net/http"
	"pvlogger-fusionsolar/internal/auth"
	"pvlogger-fusionsolar/internal/model"

	"pv-logger/logging"
)

func WithBearerToken(token auth.Token) model.ClientOption {
	return model.WithRequestEditorFn(func(ctx context.Context, req *http.Request) error {
		logging.Info("adding token to request")
		req.Header.Set("XSRF-TOKEN", token.Token)
		return nil
	})
}

func (f *FusionSolarAccess) newClient() (*model.ClientWithResponses, error) {
	return model.NewClientWithResponses(f.cfg.FsUrl(), WithBearerToken(f.token))
}
