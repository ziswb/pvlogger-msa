# 0003 — Image versioning and deployment flow

- Status: accepted (recorded retroactively; decision visible in the code)
- Date: 2026-08-06

## Context

Ten independently released components need per-component versions and a
controlled path to the cluster. Gradle/Go module versions had become meaningless
placeholders.

## Decision

- A component's version is the **`LABEL version="x.y.z"` in its Dockerfile**.
  `scripts/build.sh` scrapes it, builds with the repo root as context, tags the
  image `:latest` and `:<version>`, and pushes both to `<registry-host>:5000`
  (or a local registry with `-l`).
- What actually **runs** is pinned in the Helm chart's `values.yaml` in the
  infrastructure repo (`ansible/roles/pvlogger_helm_deploy/files/pvlogger-chart/`),
  declared there as the single source of truth for deployed versions. Deployment
  is `ansible-playbook playbooks/pvlogger.yml`.
- Release = bump the Dockerfile LABEL → `build.sh` → bump the chart tag → run the
  playbook.

## Consequences

- Version truth is split across two repos by design: this repo says what *exists*
  (registry tags), infrastructure says what *runs*.
- The `version =` fields in `build.gradle.kts` / Go modules are meaningless — do
  not bump them expecting any effect.
- `:latest` is pushed but nothing should deploy it (the chart pins explicit tags).
- ⚠️ GAP: no changelog or tag/commit convention marks releases in git; the LABEL
  bump commit is the only trace. Acceptable, or worth a lightweight convention?
