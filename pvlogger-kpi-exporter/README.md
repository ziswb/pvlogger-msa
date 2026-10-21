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

The relay runs as a **plain systemd service** (infrastructure ADR-0024,
amended 2026-10-21): his server (Ubuntu 22.04, `x86_64`) has no Docker, and the
relay is one static binary, so there is nothing else to install. Files:
`systemd/pvlogger-relay.service` and `systemd/relay.env.example`.

Build for his architecture (`uname -m`: `x86_64` → amd64) and carry it over:

```
cd pvlogger-kpi-exporter
go test ./...
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -trimpath -o build/pvlogger-kpi-exporter-<version>-amd64 ./cmd/exporter
shasum -a 256 build/pvlogger-kpi-exporter-<version>-amd64
scp build/pvlogger-kpi-exporter-<version>-amd64 systemd/* <user>@<his server>:/tmp/
```

On his server (check the SHA-256 first; `ss -ltn` shows whether the port in
`relay.env` is free — 8080 is Tomcat's default, the example uses 8780):

```
sudo install -m 0755 /tmp/pvlogger-kpi-exporter-<version>-amd64 /usr/local/bin/pvlogger-kpi-exporter
sudo useradd --system --no-create-home --shell /usr/sbin/nologin pvlogger-relay
sudo install -d -o pvlogger-relay -g pvlogger-relay -m 0755 /srv/pvlogger/export
sudo install -d -m 0700 /etc/pvlogger-relay
sudo install -m 0600 /tmp/relay.env.example /etc/pvlogger-relay/relay.env
sudoedit /etc/pvlogger-relay/relay.env      # URL, token, auth key, port
sudo install -m 0644 /tmp/pvlogger-relay.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now pvlogger-relay
journalctl -u pvlogger-relay -f             # "relay: joined the tailnet", then "listening"
```

Check: `curl -s http://localhost:8780/export` returns the JSON and a new file
appears in `/srv/pvlogger/export` (world-readable, for his platform). A 502
names the reason (upstream down, token wrong, …) and writes nothing.

- **Keep `TS_AUTHKEY` in `relay.env`.** The node identity lives in
  `/var/lib/pvlogger-relay` after the first start, but an empty `TS_AUTHKEY`
  makes the relay skip Tailscale and use the host network, where the endpoint
  is not reachable. A used single-use key cannot join anything else.
- The port listens on all interfaces. If only his own machine calls it, close
  it to the LAN with his firewall (`ufw`).
- **Update:** copy the new binary over `/usr/local/bin/pvlogger-kpi-exporter`,
  then `sudo systemctl restart pvlogger-relay`.
- **Remove:** `sudo systemctl disable --now pvlogger-relay`, delete the unit,
  `/etc/pvlogger-relay`, `/var/lib/pvlogger-relay`, `/usr/local/bin/pvlogger-kpi-exporter`
  and the user; remove the device `pvlogger-relay` in the Tailscale console.

The image (`docker/Dockerfile`, multi-arch) still runs relay mode the same way,
with the environment variables above and volumes for `KPI_EXPORT_PATH` and
`TS_STATE_DIR`, if a receiver ever has Docker.
