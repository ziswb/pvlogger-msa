# Steps Taken

```bash
go mod init pvlogger
go get -tool github.com/oapi-codegen/oapi-codegen/v2/cmd/oapi-codegen@latest
go mod tidy
go tool oapi-codegen -config internal/openapi/oapi-client.yaml internal/openapi/fusionsolar-api.yaml
go tool oapi-codegen -config internal/openapi/oapi-model.yaml internal/openapi/fusionsolar-api.yaml
```
