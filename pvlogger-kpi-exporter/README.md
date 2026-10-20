# pvlogger-kpi-exporter

Exports the latest KPI row as the `pv_*` JSON file the family PV-comparison
platform reads. One binary, three modes (`KPI_EXPORTER_MODE`, ADR-0021):

| Mode | Runs | Does |
|---|---|---|
| `export` (default) | cluster CronJob | fetch latest KPI, write `<prefix>-<timestamp>.json`, delete files older than 48 h |
| `serve` | cluster Deployment | `GET /v1/export/latest` with `Authorization: Bearer <token>` → the same JSON, file name in `Content-Disposition` |
| `relay` | receiver's server | `GET /export` → fetch from `serve` over Tailscale, write the file, clean up, return the JSON |

## Configuration

| Variable | Modes | Default |
|---|---|---|
| `TZ` | all | — (required, e.g. `Europe/Zurich`) |
| `KPI_EXPORT_PATH` | export, relay | — (required) |
| `KPI_FILE_PREFIX` | export, serve | — (required) |
| `REST_HOST` / `REST_PORT` / `REST_PATH` / `REST_PROTOCOL` | export, serve | `localhost` / `8083` / `/v1/read-latest-kpi` / `http` |
| `HTTP_PORT` | serve, relay | `8080` |
| `KPI_EXPORT_TOKEN_FILE` | serve | `/etc/pvlogger-export/token` (read per request) |
| `UPSTREAM_URL` / `UPSTREAM_TOKEN` | relay | — (required) |
| `TS_AUTHKEY` | relay | empty = no Tailscale, use the host network (testing) |
| `TS_HOSTNAME` / `TS_STATE_DIR` | relay | `pvlogger-relay` / `/var/lib/pvlogger-relay` |

## Installing the relay on the receiver's server

Build for his architecture (`uname -m`: `x86_64` → amd64) and carry it over —
the registry is reachable only at home:

```
docker build --platform linux/amd64 -f pvlogger-kpi-exporter/docker/Dockerfile -t pvlogger-kpi-exporter:<version> .
docker save pvlogger-kpi-exporter:<version> | gzip > pvlogger-kpi-exporter-<version>-amd64.tar.gz
# on his server:
docker load < pvlogger-kpi-exporter-<version>-amd64.tar.gz
```

Run it (the export directory is where his platform reads the files):

```
docker run -d --name pvlogger-relay --restart unless-stopped \
  -e TZ=Europe/Zurich \
  -e KPI_EXPORTER_MODE=relay \
  -e UPSTREAM_URL=https://pvlogger-export.<tailnet>.ts.net/v1/export/latest \
  -e UPSTREAM_TOKEN=<token> \
  -e TS_AUTHKEY=<tagged auth key> \
  -e KPI_EXPORT_PATH=/export \
  -v /srv/pvlogger/export:/export \
  -v /srv/pvlogger/state:/var/lib/pvlogger-relay \
  -p 8080:8080 \
  pvlogger-kpi-exporter:<version>
```

Check: `curl -s http://localhost:8080/export` returns the JSON and a new file
appears in `/srv/pvlogger/export`. A 502 names the reason (upstream down, token
wrong, …) and writes nothing. The auth key is only needed on the first start;
after that the node identity lives in the state volume. To bind the port to his
machine only, use `-p 127.0.0.1:8080:8080`.
