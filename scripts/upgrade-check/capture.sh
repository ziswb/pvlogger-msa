#!/usr/bin/env bash
#
# Script: capture.sh <name> [snapshot|screens]...
# Purpose: Record the current working tree's behaviour into build/upgrade-check/<name>:
#          service snapshot (snapshot.sh) and dashboard screenshots + live check
#          (screenshots.sh). Default: both. Typical use, see docs/UPGRADING.md:
#            scripts/upgrade-check/capture.sh before      # on the unchanged branch
#            ... upgrade ...
#            scripts/upgrade-check/capture.sh after
#            scripts/upgrade-check/compare.sh before after
# Dependencies: see snapshot.sh and screenshots.sh
set -euo pipefail
DIR=$(dirname -- "${BASH_SOURCE[0]}")
source "$DIR/lib.sh"
NAME=${1:?usage: capture.sh <name> [snapshot|screens]...}; shift
PARTS=("${@:-snapshot screens}")
OUT="$OUT_ROOT/$NAME"
for part in ${PARTS[*]}; do
  case $part in
    snapshot) "$DIR/snapshot.sh" "$OUT" ;;
    screens) "$DIR/screenshots.sh" "$OUT" ;;
    *) echo "unknown part: $part" >&2; exit 1 ;;
  esac
done
git -C "$REPO" log -1 --format='%h %s' > "$OUT/commit.txt"
git -C "$REPO" status --short >> "$OUT/commit.txt"
echo "captured: $OUT"
