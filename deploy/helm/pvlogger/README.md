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
| `global.imageRegistry` | | Registry host and owner path in front of the image repositories, which are the bare module names (default `ghcr.io/ziswb`, [ADR-0026](../../../docs/adr/0026-image-names-registry-carries-owner-ghcr-default.md)). The GHCR images are not published yet (backlog 53 part 2): until then, build and push them to your registry (`scripts/build-all.sh`) and set it here, e.g. `<registry-host>/<owner>` |
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
| Network policies | on (`networkPolicies.enabled`) | A policy controller that admits node-local traffic (kubelet probes), as k3s's kube-router does |

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

Steps ([ADR-0027](../../../docs/adr/0027-chart-tag-publishes-images-and-chart-to-ghcr.md)):

1. Fix commit(s), then one bump commit as above.
2. Push the commits; once `chart.yml` is green, tag the bump commit
   `chart/vX.Y.Z` and push the tag.
3. The tag runs `.github/workflows/release.yml`: every pinned image that is not
   on GHCR yet is built for `linux/amd64` and `linux/arm64` and pushed as
   `ghcr.io/ziswb/<module>:<version>`, then the chart as
   `oci://ghcr.io/ziswb/charts/pvlogger`. Versions already there are kept, so a
   failed run can be rerun; the workflow can also be started by hand for an
   existing tag.
4. The installation pins that chart version and deploys it (in this project: the
   private infrastructure repository).

Until production pulls from GHCR, it still gets its images from a private
registry: `scripts/build.sh <module>` (or `scripts/build-all.sh`) builds and
pushes them there before step 4. The registry comes from `DOCKER_REGISTRY` in
`~/.config/pvlogger/local.env`; the platform defaults to `linux/arm64`
(`PVLOGGER_PLATFORM` overrides it). It refuses to overwrite an existing tag
without `-f`.

`PersistentVolume`s have `Retain`: uninstalling the release leaves the dumps on
the NFS export.

## Upgrading

**To 4.0.0** ([ADR-0026](../../../docs/adr/0026-image-names-registry-carries-owner-ghcr-default.md)):
the image repositories lost their owner prefix (`bziswiler/pvlogger-frontend` is
now `pvlogger-frontend`), and `global.imageRegistry` defaults to `ghcr.io/ziswb`.
An install that set `global.imageRegistry: <registry-host>` sets
`<registry-host>/bziswiler` instead to keep pulling the same images. Nothing
else changed since 3.0.2.
