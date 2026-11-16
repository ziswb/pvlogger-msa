# 0026 — Image names: the registry value carries the owner; the default is GHCR

- Status: accepted (2026-11-16)
- Date: 2026-11-16

The chart's image repositories were `bziswiler/<module>` with an empty
`global.imageRegistry`, so a default install pulled from Docker Hub, from a
namespace this project does not own: anyone registering it could serve images to
people trying the public chart. The repositories are now the **bare module
names** (`pvlogger-frontend`, `pv-fast-data-collector`, …) and
`global.imageRegistry` carries host and owner, **default `ghcr.io/ziswb`**, the
project's GitHub Container Registry namespace, where releases will be published
(backlog 53 part 2). One value moves every image to another registry or mirror.

## Consequences

- Tilt builds its local images under the default names (`ghcr.io/ziswb/<module>`)
  and swaps them in; it neither pulls nor pushes, and `values-dev.yaml` needs no
  registry override.
- Until the GHCR images exist, a default install fails to pull (ImagePullBackOff)
  rather than pulling someone else's image; the chart README says to push the
  images to your own registry and set `global.imageRegistry`.
- Production keeps its private registry and its image paths: the installation sets
  `global.imageRegistry: <registry-host>/bziswiler`, and `scripts/build.sh` still
  pushes there. Moving production to GHCR (with a pull-through cache) is part of
  backlog 53 part 2.
- Dropping the prefix changes every rendered image for an install that set only a
  registry host, so this shipped as chart 4.0.0.

## Considered options

- Keep `<owner>/<module>` per repository with `ghcr.io` as the registry: works, but
  repeats the owner ten times and keeps the owner out of the one switch.
- Keep the Docker Hub default until the GHCR workflow exists: leaves the squatting
  risk in the public chart and ties the launch to that workflow.
