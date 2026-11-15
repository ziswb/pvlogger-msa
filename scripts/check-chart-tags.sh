#!/bin/sh
# Every image the chart pins must be the version its Dockerfile builds (ADR-0024:
# one bump commit changes the Dockerfile LABEL and the tag in values.yaml). Run by
# .github/workflows/chart.yml; also usable locally from the repo root.
set -eu
values=deploy/helm/pvlogger/values.yaml
status=0
# "repository: bziswiler/<module>" followed by "tag: <version>"
pairs=$(awk '
  /repository: *"bziswiler\// { gsub(/.*bziswiler\/|"/, ""); repo = $0; next }
  repo != "" && /tag:/ { gsub(/.*tag: *|"/, ""); print repo, $0; repo = "" }
' "$values")
[ -n "$pairs" ] || { echo "no bziswiler/* images found in $values" >&2; exit 1; }
echo "$pairs" | while read -r module tag; do
  dockerfile="$module/docker/Dockerfile"
  if [ ! -f "$dockerfile" ]; then
    echo "::error file=$values::$module: no $dockerfile"
    exit 1
  fi
  label=$(sed -n 's/^LABEL version="\(.*\)"/\1/p' "$dockerfile")
  if [ "$label" != "$tag" ]; then
    echo "::error file=$values::$module: chart tag $tag, Dockerfile LABEL version $label"
    exit 1
  fi
  echo "$module $tag"
done || status=1
exit $status
