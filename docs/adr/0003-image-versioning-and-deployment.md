# 0003 — Image versioning and deployment flow

- Status: deployment part superseded by 0024 (2026-11-15); the image-version part stands (was: accepted, recorded retroactively; decision visible in the code)
- Date: 2026-08-06

## Context

Ten independently released components need per-component versions and a
controlled path to the cluster. Gradle/Go module versions had become meaningless
placeholders.

## Decision

- A component's version is the **`LABEL version="x.y.z"` in its Dockerfile**.
  `scripts/build.sh` scrapes it, builds with the repo root as context, tags the
  image `:<version>` only, and pushes it to the registry in `DOCKER_REGISTRY`
  (`<registry-host>:5000`, set outside the repo in `~/.config/pvlogger/local.env`),
  or to a local registry with `-l`. It refuses a version that is already in the registry
  unless forced with `-f`.
- What actually **runs** is pinned in the Helm chart's `values.yaml` in the
  infrastructure repo (`ansible/roles/pvlogger_helm_deploy/chart/`),
  declared there as the single source of truth for deployed versions.
  *(Superseded by ADR-0024: the chart and its pins live in this repo.)* Deployment
  is `ansible-playbook playbooks/pvlogger.yml`.
- Release = bump the Dockerfile LABEL → `build.sh` → bump the chart tag → run the
  playbook.

## Consequences

- Version truth is split across two repos by design: this repo says what *exists*
  (registry tags), infrastructure says what *runs*. *(Superseded by ADR-0024: this
  repo's chart pins what runs; infrastructure pins only the chart version.)*
- The `version =` fields in `build.gradle.kts` / Go modules are meaningless — do
  not bump them expecting any effect.
- No `:latest` tag (dropped 2026-10-23, infrastructure backlog 58): nothing
  deployed it, and each push re-pointed it and left an untagged manifest in the
  registry. The chart fails on a missing tag instead of falling back to `latest`.
- ⚠️ GAP: no changelog or tag/commit convention marks releases in git; the LABEL
  bump commit is the only trace. Acceptable, or worth a lightweight convention?
