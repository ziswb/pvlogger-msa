# 0028 — Renovate proposes dependency updates, grouped per ecosystem

- Status: applied 2026-11-26 (accepted the same day; Renovate's GitHub app
  installed, `renovate.json5` on `main` with chart 5.1.2)
- Date: 2026-11-26

## Context

Dependency updates follow `docs/UPGRADING.md`: patch and minor bumps grouped per
ecosystem, one commit each; majors and toolchain changes only after the owner
approved them from a table. Finding what is behind (the inventory step) was a
manual survey of Maven Central, the Go proxy, npm and the registries, which is
why it happened rarely. Dependabot opens PRs only for the GitHub Actions pins
(backlog 60); for the other ecosystems only its security updates.

The production site that installs the chart is moving to updates proposed by
Renovate and merged by the owner; its pins on this repository are the chart
version, so the images built here reach production only through a chart
release the owner tags (ADR-0024, ADR-0027).

## Decision

- **Renovate's GitHub app proposes the updates** (`renovate.json5`), weekly
  (Monday early morning, UTC), and never merges.
- **Grouped as `docs/UPGRADING.md` groups them:** one PR per ecosystem for
  patch and minor: *JVM libraries* (`gradle/libs.versions.toml`), *Go modules*
  (every `go.mod`, with `go mod tidy`), *Frontend (npm)*, *Base images* (every
  `FROM`, tag and digest together; a new digest for the same tag included),
  *Chart images* (the third-party images in `values.yaml`, each marked by a
  `# renovate:` comment above it).
- **Held on the Dependency Dashboard until the owner ticks them:** every
  major, each on its own; the Go toolchain beyond a patch (the `go` lines and
  the `golang` image, one PR); Gradle itself. That is the approval step of
  `docs/UPGRADING.md`, the table becomes the Dashboard.
- The Postgres client image (`dbBackup.image`) never gets a major from
  Renovate: it follows production's major, which the infrastructure moves.
- Dependabot keeps the GitHub Actions pins. The CI tool versions (golangci-lint,
  kubeconform, helm-unittest, gitleaks with their checksums), the JDK
  language level and the test/dev Postgres stay with `docs/UPGRADING.md`.
- `docs/UPGRADING.md`'s checks still apply to a Renovate PR before it is
  merged: CI on the PR, and for a group that touches runtime behaviour the
  before/after capture.

## Consequences

- The inventory step is gone; what is behind is the open PRs and the
  Dashboard.
- At most five PRs a week for patch and minor, plus the majors the owner
  approves.
- A merged update changes nothing in production by itself: it ships with the
  next chart tag.
- Dependabot's security updates for Go, npm and Gradle would duplicate
  Renovate's PRs; they stay off (`gh api repos/{owner}/{repo}/automated-security-fixes`
  answers `"enabled": false`) and the alerts stay on, which Renovate reads for
  its own vulnerability PRs.
