# Steps Taken

```bash
go mod init pvlogger
go get -tool github.com/oapi-codegen/oapi-codegen/v2/cmd/oapi-codegen@latest
go mod tidy
go tool oapi-codegen -config internal/openapi/oapi-client.yaml internal/openapi/fusionsolar-api.yaml
go tool oapi-codegen -config internal/openapi/oapi-model.yaml internal/openapi/fusionsolar-api.yaml
```

If the generator doesn't work, this is what Windsurf suggested:

> Fix applied: dependency pinning (and it works)
I added a replace in app/go.mod to pin github.com/dprotaso/go-yit to the commit that oapi-codegen v2.5.1 (and yaml-jsonpath v0.3.2) expect:
> ```aiignore
> replace github.com/dprotaso/go-yit => github.com/dprotaso/go-yit v0.0.0-20220510233725-9ba8df137936
> ```
> So the root cause was not your Go version, but a dependency skew: your module had pulled in a newer go-yit that uses
YAML v4 node types, while yaml-jsonpath (used by the generator stack) expects YAML v3 node types.