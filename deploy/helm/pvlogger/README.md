# PVLogger Helm chart

Deploys all PVLogger components into one namespace. The chart pins every image
tag, so a chart version names a tested set of component versions
([ADR-0024](../../../docs/adr/0024-helm-chart-lives-here-site-values-stay-private.md)).
Releases are tagged `chart/vX.Y.Z`.

## Values

[`values.yaml`](values.yaml) has complete generic defaults (ports, schedules,
resources, log levels). The in-cluster service hosts are derived from the release
namespace. What only your installation knows goes into your own values file:

| Key | Required | What |
|---|---|---|
| `global.imageRegistry` | | Registry in front of the image repositories (empty: Docker Hub) |
| `global.timezone` | | Zone of the cron schedules and the containers (default `UTC`) |
| `global.runAsUid` | | Run-as uid/gid of the Go jobs (default `1000`) |
| `secrets.postgresPassword` | yes | Password of `database.username` |
| `database.host` | yes | PostgreSQL holding `pvdata`, `pvmasterdata`, `fsdata` |
| `modbus.host` | yes | The Modbus TCP gateway (SmartDongle) |
| `modbus.meterDeviceIds`, `modbus.inverterDeviceIds`, `modbus.dongleDeviceId` | | Unit IDs behind the gateway |
| `pushgateway.enabled`, `pushgateway.host` | | Prometheus Pushgateway for the collectors' run metrics |

Optional components, all off by default except the network policies:

| Component | Turn on with | Also needs |
|---|---|---|
| FusionSolar API jobs | `fusionsolar.enabled`, `fusionsolarAuth.enabled` | `secrets.fusionsolarPassword`, `fusionsolar.username`/`stationCode`/`devTypeId`, `fusionsolarAuth.username`/`fsHost` |
| Ingress (Traefik + cert-manager) | `ingress.enabled`, `ingress.traefik.enabled` | `ingress.traefik.host`, `ingress.traefik.clusterIssuer` (unless `ingress.traefik.tls` is false: plain HTTP, dev only) |
| Ingress (Tailscale operator) | `ingress.enabled`, `ingress.tailscale.enabled` | `ingress.tailscale.hostname` |
| KPI export endpoint | `kpiExportServe.enabled` | `secrets.kpiExportToken`, `kpiExportServe.tailscaleHostname`/`tailscaleTag` |
| Nightly DB dump to NFS | `dbBackup.enabled` | `dbBackup.persistence.nfsServer`/`nfsPath` |
| Data-freshness heartbeat | `heartbeat.enabled` | `secrets.heartbeatPingUrl` |
| Image pre-pull on every node | `imagePrepull.enabled` | Kubernetes with image volumes |
| Modbus mock server (dev only) | `modbusMockServer.enabled` | `modbus.host: pvlogger-modbus-mock-server-service`, so the collectors read it |
| Backdated raw rows for the history backfill (dev only) | `dataAccessor.devAcceptClientTimestamps` | `modbusMockServer.enabled` (the render fails without it); the backfill Job itself is `deploy/dev/backfill-job.yaml` |
| Network policies | on (`networkPolicies.enabled`) | `networkPolicies.kubeletCidr` if your pod network is not k3s's `10.42.0.0/16` |

A missing required value fails the render with a message that names the key.
The ingress exposes an allowlist only ([ADR-0020](../../../docs/adr/0020-trust-boundary-ingress-allowlist.md)).

Secrets are rendered from values (an `existingSecret` option is backlog item 54).
The database roles must exist before the first install (ADR-0023); migrations
only grant to them.

## Local development

[`deploy/dev/values-dev.yaml`](../../dev/values-dev.yaml) holds the values for the
local Tilt environment (dummy credentials, the mock server as the Modbus device).

## Release

A component's bump commit changes its Dockerfile `LABEL version`, its tag in
`values.yaml` and `version` in `Chart.yaml` together. The chart's bump is the
highest bump among its changes: an image patch is a chart patch; an image minor
or a new values key is a minor; a removed or renamed key, or a changed default
that existing installs notice, is a major. CI fails when the chart changed but
its version did not.

`PersistentVolume`s have `Retain`: uninstalling the release leaves the dumps on
the NFS export.
