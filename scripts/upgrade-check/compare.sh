#!/usr/bin/env bash
#
# Script: compare.sh <before> <after>
# Purpose: Compare two captures in build/upgrade-check/. Service snapshots must be identical
#          (only the HTTP Date header is ignored). Screenshots are compared pixel by pixel
#          and the live-check output line by line. Exit code 1 if any of those differ; a
#          deliberate change (e.g. a fixed label) shows up too and needs a human look.
#          The rendered DOM is diffed for information only (a chart library update rewrites
#          its SVG internals without visible change); the full diff goes to <after>/screens/dom.diff.
# Dependencies: python3 (creates a venv with Pillow under build/upgrade-check/.venv on first use)
set -uo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/lib.sh"
A="$OUT_ROOT/${1:?usage: compare.sh <before> <after>}"; B="$OUT_ROOT/${2:?usage: compare.sh <before> <after>}"
status=0

if [ -d "$A/snapshot" ] && [ -d "$B/snapshot" ]; then
  echo "== service snapshot"
  if diff -r -x logs -I '^Date: ' "$A/snapshot" "$B/snapshot"; then echo "identical"; else status=1; fi
  [ -s "$B/snapshot-log-findings.txt" ] && echo "(log findings to read: $B/snapshot-log-findings.txt)"
fi

if [ -d "$A/screens" ] && [ -d "$B/screens" ]; then
  echo "== screenshots + DOM"
  VENV="$OUT_ROOT/.venv"
  [ -x "$VENV/bin/python" ] || { python3 -m venv "$VENV" && "$VENV/bin/pip" install -q --disable-pip-version-check pillow; }
  out=$("$VENV/bin/python" "$CHECK_DIR/compare-screens.py" "$A/screens" "$B/screens")
  echo "$out" | sed -n '/^DOM: /q;p'
  echo "$out" | sed -n '/^DOM: /,$p' | tail -n +2 > "$B/screens/dom.diff"
  echo "$(echo "$out" | grep '^DOM: ') (information only, see $B/screens/dom.diff)"
  echo "$out" | grep -qE ': [1-9][0-9]* px differ|size differs' && status=1
  echo "== live check"
  if diff "$A/screens/live-check.txt" "$B/screens/live-check.txt"; then
    echo "identical"
  else status=1; fi
  grep -q "LIVE UPDATE OK" "$B/screens/live-check.txt" || { echo "live update did not reach the cards in $2"; status=1; }
fi
exit $status
