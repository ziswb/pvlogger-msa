# Upgrading dependencies and tooling

This is the standing procedure for "update everything" requests, so it doesn't need to be agreed on each time.
It covers libraries, major versions and tooling (JDK, Go, Gradle, Node/Angular, base images).

## Ground rules

- Work on a branch. Nothing is pushed, merged or deployed without the owner's go-ahead.
  Rollout: the owner tags the chart (`chart/vX.Y.Z`, ADR-0024) and the sibling `infrastructure` repo bumps its
  pinned chart version; the owner watches the first CronJob runs. Optional, by hand: once the release workflow
  has published the images, `scripts/scan-published-images.py` checks their contents with the private blocklist,
  which the git hooks never see (security finding 65).
- **Major versions and toolchain changes need approval first.** Before any work, present a table with
  one row per major/toolchain bump: from → to, pros, cons/breaking changes, risk (low/med/high),
  and the effort expected. The owner picks which ones go ahead. Patch and minor bumps don't need approval.
- One concern per commit: patch/minor bumps grouped per ecosystem, each major on its own.
  This keeps `git bisect` and reverting cheap.
- Versions for the JVM services live only in `gradle/libs.versions.toml` (see CLAUDE.md).
- Follow the ADRs. An upgrade is not the moment to bring in new patterns. If an upgrade forces one,
  write it up as an ADR.

## Where versions live

| Area | Location |
|---|---|
| JVM libraries + Kotlin plugin | `gradle/libs.versions.toml` |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` (`./gradlew wrapper --gradle-version X`) |
| JDK | `JavaLanguageVersion.of(N)` in every `*/build.gradle.kts`, `FROM gradle:*-jdkN-*` and `eclipse-temurin:N-*` in `*/docker/Dockerfile` |
| Go libraries | every `*/go.mod` (and `go.work.sum`) |
| Go toolchain | `go.work`, each `go.mod` `go` line, `FROM golang:*` in Dockerfiles, `.golangci.yml` / golangci-lint version |
| Frontend | `pvlogger-frontend/src/web/package.json` (+ lockfile) |
| Base images | Every `FROM` in `*/docker/Dockerfile` is pinned as `image:tag@sha256:<digest>` (backlog 58): `golang`, `distroless/static-debian*:nonroot`, `gradle:*-jdk*-alpine`, `eclipse-temurin:*-ubi*-minimal`, `node:*-alpine`. Change tag and digest together; the digest is the multi-arch index's (`docker buildx imagetools inspect <image:tag>`). A new digest for the same tag (a rebuilt base) is a patch bump of every image built on it. The chart's third-party images (`dbBackup.image`, `heartbeat.image` in `values.yaml`, the dev `deploy/dev/postgres.yaml`) are pinned the same way, a chart bump; `imagePrepull.holdImage` stays tag-only (see its comment) |
| CI | `.github/workflows/*.yml`: action SHAs (+ tag comment; Dependabot proposes these weekly in one grouped PR, `.github/dependabot.yml`), `java-version`, golangci-lint `version`, `node-version`, Helm `version`, `KUBECONFORM_VERSION` + `KUBECONFORM_SHA256`, `HELM_UNITTEST_VERSION` + `HELM_UNITTEST_SHA256`, `GITLEAKS_VERSION` + `GITLEAKS_SHA256` (`go-version-file: go.work` follows by itself) |
| Chart images | `deploy/helm/pvlogger/values.yaml`: `dbBackup.image` (Postgres client, same major as production), `heartbeat.image` (curl), `imagePrepull.holdImage` (pause) |
| Tilt | `deploy/dev/postgres.yaml` (dev Postgres image), the Tiltfile's `version_settings` constraint |
| Test/dev DB | Testcontainers `postgres:18` (3 test bases), `pvlogger-database/docker/docker-compose.yaml`, `pvlogger-database/scripts/run-db-container.sh`, `deploy/dev/postgres.yaml` |

Not in scope here: the CNPG/PostgreSQL server version and the cluster setup. Those belong to the `infrastructure` repo.
The test and dev Postgres versions should match production's major version.

## Procedure

1. **Baseline (before changing anything)**
   - Rancher Desktop running (Testcontainers and the check scripts need Docker).
   - `./gradlew cleanTest build` (includes Go build and lint), `make test` (every Go module, libraries included),
     `npm ci && npm run build:ci` in `pvlogger-frontend/src/web`, `npm audit`.
     Use `cleanTest`: a cached `test` task proves nothing.
   - Write down tests that already fail, so they aren't blamed on the upgrade.
   - Record the current behaviour: `scripts/upgrade-check/capture.sh before` (see below).
2. **Inventory**: there is no Gradle versions plugin; look up the catalog entries on Maven Central
   (`maven-metadata.xml`) and the Gradle wrapper on services.gradle.org. Then `go list -m -u all` per module
   (it does not show new `/vN` major paths), `npm outdated`, and the base image tags (`docker manifest inspect`).
   Sort into patch/minor and major/toolchain.
3. **Proposal**: the major/toolchain table from the ground rules. Wait for approval.
4. **Patch/minor upgrades**, per ecosystem, with a full test run after each.
5. **Approved majors**, one at a time: read the changelog/migration guide, upgrade, fix, test, commit.
   Angular goes one major at a time with `ng update`. Toolchain bumps update every place in the table above together.
   Run `scripts/upgrade-check/capture.sh after-<step>` + `compare.sh before after-<step>` after each step:
   it is cheap and pins a regression to one commit.
6. **Behaviour checks**. These go beyond "tests pass" because they break quietly:
   - `scripts/upgrade-check/capture.sh after` and `compare.sh before after` must report everything identical:
     REST and error responses, the kpi-exporter `<keyPrefix>_*` JSON and its file name (the receiver's relay depends
     on it, ADR-0021), the first `/livedata` frame, Prometheus metric names, Flyway history and schema,
     dashboard screenshots (pixels) and the live browser check. Read `snapshot-log-findings.txt` too.
   - Flyway or migration changes: `scripts/upgrade-check/flyway-takeover.sh before` (old jars migrate, new jars
     take over the same database — the production case).
   - DB connection timeouts/keepalive still apply (backlog 45), TLS to the DB still works (ADR-0023).
   - Every Docker image builds and its container starts (`docker build -f <service>/docker/Dockerfile .`).
   - Your own look at the dashboard: the scripts compare pixels, they don't judge whether a change is right.
7. **Version bumps**: every image whose content changed gets a new `LABEL version` in its Dockerfile
   (ADR-0003), and the same commit sets that tag in `deploy/helm/pvlogger/values.yaml` and bumps `version` in
   `Chart.yaml` (ADR-0024; the chart's bump is the highest among its changes, see the chart README). CI
   (`chart.yml` via `ci.yml`, `scripts/check-chart-tags.sh`) fails when a Dockerfile and its chart tag disagree. A dependency or base-image change counts, even without code changes. Otherwise
   the release workflow skips the image (the version is already on GHCR, ADR-0027) and production keeps the old
   build. Released versions are never overwritten, so the old tag stays the rollback target. Patch for dependency/base-only changes,
   minor for a runtime or framework major (JDK, Angular). Before committing, check the tags are free:
   `docker manifest inspect ghcr.io/ziswb/<module>:<version>` → `manifest unknown`.
8. **Report**: what changed (from → to), what was skipped and why, how each item was tested, what can only
   be checked in production (real Modbus device, FusionSolar API, K8s deploy), and the rollout and rollback notes
   (the chart version to tag and pin in the infrastructure repo).

## Check scripts (`scripts/upgrade-check/`)

| Script | What it does |
|---|---|
| `capture.sh <name> [snapshot\|screens]` | Records the working tree into `build/upgrade-check/<name>`: both parts by default |
| `snapshot.sh` | Data accessor, masterdata, frontend (Ktor) and kpi-exporter against a throwaway Postgres 18 with fixed data; saves every response, metric names, Flyway history, schema, and the jars |
| `screenshots.sh` | `ng serve` against the real backend with "today" data; desktop/phone screenshots, rendered DOM, and `live-check.mjs` (WebSocket card update, day navigation) in headless Chrome |
| `compare.sh <a> <b>` | Diff of two captures; exit 1 on any difference except the DOM, which is information only |
| `flyway-takeover.sh <name>` | The captured old jars migrate a fresh database, the current jars start on it |

They never touch the local dev database or the cluster. `screenshots.sh` needs ports 8083, 8085 and 4300 free
and Google Chrome; capture `before` and `after` on the same day, because the dashboard opens on today.
To capture an older commit, run the scripts from a `git worktree` of it (copy `scripts/upgrade-check/` in if
the commit predates them) and copy its `build/upgrade-check/<name>` over.

## Pitfalls seen (2026-11)

- Green tests are not a running service: Koin 4.2 pulled in Ktor's DI plugin, every service failed at startup,
  and all tests still passed because they start the app differently. The snapshot caught it.
- Library defaults change visibly: Highcharts 13 turned the axis date label into "12:00 AM". The pixel compare
  caught it; a unit test now guards it.
- `ng update` for several Angular packages at once can mix majors; update core + CLI first, then Material + CDK.
  Keep the migrations that preserve behaviour (explicit zone change detection, `Eager`, `withXhr()`).
- `karma-jasmine` 5.1 (last release) pinned Jasmine 4, so the frontend moved to Vitest (jsdom).
  - Angular's `refactor-jasmine-vitest` schematic only deleted the `done()` calls, so those tests could no longer fail.
    Rewrite them by hand with `firstValueFrom`.
  - Under Vitest, highcharts-angular's default loader fails in Node, and "should create" chart specs still pass.
    The chart specs now assert that a chart was drawn (see the frontend README).
- The version bumps were first forgotten and `build-all.sh` skipped every image, hence step 7.
- `JAVA_HOME` matters for Gradle 8 (runs only on ≤ 21); Gradle 9 runs on the default JDK.

## Known gaps

- There are no automated end-to-end tests. `tilt up` (CLAUDE.md, Local Development) runs the collector → data
  accessor → dashboard path against the synthetic plant; use it to check an upgrade before the first production run.
