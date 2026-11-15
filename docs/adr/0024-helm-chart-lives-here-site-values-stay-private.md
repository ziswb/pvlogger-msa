# 0024 — The Helm chart lives here and pins the deployed versions; site values stay private

- Status: accepted (2026-11-15); the move is backlog item 52
- Date: 2026-11-15
- Supersedes: the deployment part of 0003 (the image-version part stands)

## Context

The chart lived in the private `infrastructure` repo, inside the Ansible role
that installs it. Its `values.yaml` pinned the image tags, and almost every
other value, generic or not, came from the role's values template. So the chart
could not be installed without that role, and it was versioned apart from the
code it deploys. This repo is now public (backlog 51): Tilt (backlog 23) and
readers need a chart they can install on their own, while the site specifics
(hosts, device IDs, accounts, secrets) must stay private.

## Decision

1. **The chart lives in `deploy/helm/pvlogger/` and pins every image tag.**
   A **chart version** therefore names a tested set of image versions plus
   templates. Infrastructure pins only the chart version it installs.
2. **Versioning:** tag `chart/vX.Y.Z`, first version `1.0.0`, no `appVersion`
   (no single app version exists). The chart's bump is the highest bump among
   its changes: an image patch is a chart patch; an image minor or a new values
   key is a minor; a removed or renamed key, or a changed default that existing
   installs notice, is a major. A component's bump commit changes its Dockerfile
   `LABEL`, its tag in the chart values and `Chart.yaml` `version` together. CI
   (`chart.yml`) fails when the chart changed but its version did not.
3. **Values split:** generic settings (ports, schedules, resources, log levels,
   enabled flags) are complete chart defaults. Names that follow from the
   install (in-cluster service hosts, the backup's read-only DB host) are
   computed in templates. **Site values** stay in the infrastructure repo; the
   chart has neutral defaults for them (empty, `UTC`, `example.invalid`) and
   documents them in its README.
4. **Homelab templates stay in the chart, off by default:** heartbeat, image
   pre-pull, DB backup, Traefik and Tailscale ingress, KPI export serve. Network
   policies are **on** by default, with the k3s pod network as the default
   kubelet CIDR.
5. **Secrets keep coming from values**; `required` fails a render without a DB
   password. An `existingSecret` option is backlog item 54.
6. **The dev database is not in the chart.** Tilt deploys a plain Postgres
   from `deploy/dev/`, next to `values-dev.yaml` (outside the chart directory,
   so it is never packaged).

## Consequences

- Versions are no longer split across two repos: this repo says what exists
  *and* which set belongs together; infrastructure picks a chart version.
- Every component release is also a chart release. Two tags, one bump commit.
- Chart history before the move stays in the private repo; it was copied, not
  imported (its commit messages carry site context).
- Until the release workflow publishes `oci://ghcr.io/ziswb/charts/pvlogger`
  (backlog 53), infrastructure extracts the chart from a local clone at the
  pinned tag (infrastructure ADR-0032).
