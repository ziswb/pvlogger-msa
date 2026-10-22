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
`systemd/pvlogger-relay.service`, `systemd/relay.env.example` and
`systemd/install.sh`.

Build the bundle on the Mac (binary, unit, `relay.env` template, `install.sh`,
`SHA256SUMS`) and carry it over:

```
cd pvlogger-kpi-exporter
make relay-bundle VERSION=<version>          # RELAY_ARCH=arm64 for an ARM host
scp build/pvlogger-relay-<version>-amd64.tar.gz <user>@<his server>:
```

On his server: unpack, fill in `UPSTREAM_TOKEN` and a single-use, pre-approved
`TS_AUTHKEY` for `tag:pvlogger-relay` (made just before) in `relay.env`, run
the script with the export directory and port:

```
tar -xzf pvlogger-relay-<version>-amd64.tar.gz && cd pvlogger-relay-<version>-amd64
nano relay.env
sudo ./install.sh -d /srv/pvlogger/export -p 8780
```

`install.sh` checks the checksums, architecture, env file and that the port is
free (8080 is Tomcat's default), creates the system user `pvlogger-relay`
(UID picked by the system), the export directory, `/etc/pvlogger-relay/relay.env`
(root, 0600) and a drop-in `pvlogger-relay.service.d/site.conf` with the
directory and port, starts the unit, waits for "listening", calls `/export`
once and then deletes the filled-in `relay.env` from the bundle.

- **The export directory must be the relay's own.** Each call deletes *every*
  regular file older than 48 hours in it (as the CronJob does), so the script
  refuses a non-empty directory that belongs to someone else, and paths the
  sandbox hides (`/home`, `/tmp`, …).
- **`-g GROUP`** (optional, an existing group such as his platform's): the
  directory becomes `pvlogger-relay:GROUP` 2775 and files 0664, so his side can
  also delete or move files. Without it, files are 0644 (readable by all).
- **Update:** run the new bundle's `install.sh` with the same `-d`/`-p`; with
  the untouched `relay.env` template it keeps the installed secrets.
- **Remove:** `sudo ./install.sh -u` (keeps the export directory), then remove
  the device `pvlogger-relay` in the Tailscale console.

Check by hand: `curl -s http://localhost:8780/export` returns the JSON and a new
file appears in the export directory. A 502 names the reason (upstream down,
token wrong, …) and writes nothing.

`install.sh` with bundle 3.5.0 tested 2026-10-22 on the same laptop: ran
through (operator).

Tested 2026-10-21 on an Ubuntu 26.04 `x86_64` laptop with this unit unchanged:
tsnet joined in under a second inside the sandbox, `/export` 200 with the file
written (0644), and a restart rejoined from the state directory without using
the key ("Ignoring authkey"). `systemd-analyze security`: 3.1.

- **Keep `TS_AUTHKEY` in `relay.env`.** The node identity lives in
  `/var/lib/pvlogger-relay` after the first start, but an empty `TS_AUTHKEY`
  makes the relay skip Tailscale and use the host network, where the endpoint
  is not reachable. A used single-use key cannot join anything else.
- The port listens on all interfaces. If only his own machine calls it, close
  it to the LAN with his firewall (`ufw`).

The image (`docker/Dockerfile`, multi-arch) still runs relay mode the same way,
with the environment variables above and volumes for `KPI_EXPORT_PATH` and
`TS_STATE_DIR`, if a receiver ever has Docker.

### Testing a bundle on a spare Ubuntu machine

Before a visit, run the new bundle once on a spare `x86_64` Ubuntu machine (the
operator's laptop).

- **Use a separate test auth key**, not the single-use key meant for his site.
  An **ephemeral** key is fine: Tailscale removes that device by itself afterwards.
- **Clear an earlier install first.** If the test machine's `pvlogger-relay`
  device is still in the tailnet, remove it in the Tailscale console, or his
  relay comes up as `pvlogger-relay-1`.
- On success the script deletes the filled-in `relay.env`. That's intended.

Install:

```
scp build/pvlogger-relay-<version>-amd64.tar.gz <test machine>:
tar -xzf pvlogger-relay-<version>-amd64.tar.gz && cd pvlogger-relay-<version>-amd64

sudo ./install.sh -u                                # removes an earlier install (tests removal too)
nano relay.env                                      # token + test auth key
sudo ./install.sh -d /srv/pvlogger/export -p 8780   # expect "200 OK, newest file: …"
```

Cases that must keep the secrets or refuse:

```
tar -xzf ../pvlogger-relay-<version>-amd64.tar.gz -C /tmp && cd /tmp/pvlogger-relay-<version>-amd64
sudo ./install.sh -d /srv/pvlogger/export -p 8780   # untouched template → "keeping the installed …", 200 again

python3 -m http.server 8781 &                       # occupy a port
sudo ./install.sh -d /srv/pvlogger/export -p 8781   # → "port 8781 is already in use"; then: kill %1

sudo mkdir -p /srv/foreign && sudo touch /srv/foreign/x
sudo ./install.sh -d /srv/foreign -p 8780           # → "not empty and belongs to root"
sudo ./install.sh -d /home/$USER/x -p 8780          # → "sandbox hides"
```

Optional: run with `-g <some group>` and check that `ls -l /srv/pvlogger/export`
shows that group and that new files are `-rw-rw-r--`.

Clean up:

```bash
cd ~/pvlogger-relay-<version>-amd64   # if gone: tar -xzf ~/pvlogger-relay-<version>-amd64.tar.gz -C ~ first
sudo ./install.sh -u                  # service, unit, drop-in, /etc/pvlogger-relay, state, binary, user
cd ~
sudo rm -rf /srv/pvlogger /srv/foreign   # -u keeps the export directory on purpose
rm -rf ~/pvlogger-relay-<version>-amd64 /tmp/pvlogger-relay-<version>-amd64

systemctl status pvlogger-relay       # "could not be found"
id pvlogger-relay                     # "no such user"
ls /etc/pvlogger-relay /var/lib/pvlogger-relay /usr/local/bin/pvlogger-kpi-exporter /srv/pvlogger 2>&1   # all "No such file"
```

If the test key wasn't ephemeral, remove the test machine's `pvlogger-relay`
device in the Tailscale console. Note the bundle's checksum
(`sha256sum pvlogger-relay-<version>-amd64.tar.gz`) so you can confirm that the
copy you take to his site is the one you tested.
