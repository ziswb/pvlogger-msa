# 0027 — The chart tag publishes the images and the chart to GHCR

- Status: accepted (2026-11-16); backlog item 53 part 2
- Date: 2026-11-16

## Context

A release was a bump commit, `scripts/build.sh` per changed image from a
workstation (arm64 only, into the private registry), and the tag `chart/vX.Y.Z`
(ADR-0024). The public chart defaults to `ghcr.io/ziswb` (ADR-0026), where
nothing was published, so a default install could not pull. Backlog 53 first
proposed one release tag per module (`<module>/vX.Y.Z`).

## Decision

1. **One trigger: the chart tag.** Pushing `chart/vX.Y.Z` runs
   `.github/workflows/release.yml`. It checks that the tag is on `main`, that
   `Chart.yaml` has that version and that every pinned image tag is its
   Dockerfile's `LABEL version` (`scripts/check-chart-tags.sh`), then publishes
   each pinned image as `ghcr.io/<owner>/<module>:<version>` for `linux/amd64`
   and `linux/arm64`, and after all images the chart as
   `oci://ghcr.io/<owner>/charts/pvlogger`. No per-module tags: the chart
   version already names the set of image versions that belong together.
2. **Published versions are immutable.** An image or chart version that is
   already in the registry is kept, not rebuilt. So an unchanged module costs
   nothing, a failed run can simply be rerun, and a dispatch for an older tag
   only fills in what is missing. Only a clear "absent" answer from the
   registry counts as absent.
3. **Build stages run on the build host.** Go cross-compiles (backlog 56); the
   Gradle and Angular stages run natively, because their output is
   platform-independent. Only the runtime stage is per platform, so QEMU runs
   just its few `RUN` steps.
4. **Images carry OCI labels** (source, revision, version, licence) added by the
   workflow, not by the Dockerfiles, so a label change is no image bump.

## Consequences

- The release flow loses its build-and-push step: bump commit, push, tag. The
  workflow only uses `GITHUB_TOKEN`; no registry credentials live in the repo.
- Production keeps `scripts/build.sh` and the private registry until it pulls
  from GHCR through that registry as a pull-through cache (infrastructure side).
  While the repository is private its packages are private too, and the
  cluster has no GHCR credentials; the switch follows the publication.
- A wrong image under a released version is fixed by a new version, never by a
  re-push; the workflow cannot overwrite one.
- GHCR packages start private; each needs its visibility set once.

## Considered options

- **A tag per module** (`<module>/vX.Y.Z`): ten more tags per release cycle and
  a second source for what belongs together, next to the chart.
- **Native arm64 runners per platform plus a manifest merge:** faster for
  emulation-heavy builds, but with native build stages almost nothing is
  emulated, and it doubles the jobs.
- **Rebuild and re-push on every tag:** simpler, but overwrites released tags
  with different digests.
