#!/usr/bin/env bash
#
# Script: flyway-takeover.sh <before>
# Purpose: The production case for a Flyway (or migration) change: the jars captured in
#          build/upgrade-check/<before>/jars migrate a fresh PostgreSQL 18 and store a KPI,
#          then the current tree's jars start on that same database. Passes when Flyway
#          validates, applies nothing unexpected, and history (incl. checksums), schema
#          and stored data are unchanged. New migrations in the current tree show up as a
#          history/schema diff, which is then expected and needs reading.
# Dependencies: docker, java (toolchain JDK), curl; a prior `capture.sh <before> snapshot`
set -uo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/lib.sh"
OLD="$OUT_ROOT/${1:?usage: flyway-takeover.sh <before>}/jars"
[ -d "$OLD" ] || { echo "no jars in $OLD - run capture.sh $1 snapshot first" >&2; exit 1; }
OUT="$OUT_ROOT/flyway-takeover-$1"; rm -rf "$OUT"; mkdir -p "$OUT"
trap cleanup EXIT

KPI='{"jobId":"fw","dateCreated":1793645100000,"producing":{"w":1234},"exporting":{"w":1},"importing":{"w":2},"consuming":{"w":3},"selfConsuming":{"w":4},"produced":{"wh":5},"exported":{"wh":6},"imported":{"wh":7},"consumed":{"wh":8},"selfConsumed":{"wh":9}}'

# run <jar> <port> <log> [seed]: start, wait, optionally store a KPI, read the latest KPI, stop.
run() {
  start_jar "$1" "$3" "${DB_ARGS[@]}" -P:ktor.deployment.port="$2"
  if wait_for "http://localhost:$2/metrics" 60; then echo "started $(basename "$1")"; else echo "FAILED to start $(basename "$1"), see $3"; fi
  if [ "$2" = 18083 ]; then
    [ "${4:-}" = seed ] && curl -s -o /dev/null -X POST -H 'Content-Type: application/json' -d "$KPI" localhost:18083/v1/store-kpi-values
    curl -s localhost:18083/v1/read-latest-kpi > "$3.latest-kpi"
  fi
  # Only one service runs at a time here.
  kill "${PIDS[0]}"; wait "${PIDS[0]}" 2>/dev/null; PIDS=()
}
state() {
  for db in pvdata pvmasterdata; do
    docker exec "$DB_CONTAINER" psql -U postgres -d $db -Atc \
      "select installed_rank, version, description, type, script, checksum, success from flyway_schema_history order by installed_rank"
    docker exec "$DB_CONTAINER" pg_dump -U postgres -s -d $db | grep -vE '^--|^$|restrict '
  done
}

build_jars
start_db
run "$OLD/pvlogger-modbus-data-accessor-all.jar" 18083 "$OUT/accessor-old.log" seed
run "$OLD/pvlogger-masterdata-server-all.jar" 18081 "$OUT/masterdata-old.log"
state > "$OUT/state-old.txt"
run "$(jar_of pvlogger-modbus-data-accessor)" 18083 "$OUT/accessor-new.log"
run "$(jar_of pvlogger-masterdata-server)" 18081 "$OUT/masterdata-new.log"
state > "$OUT/state-new.txt"

status=0
if diff "$OUT/state-old.txt" "$OUT/state-new.txt"; then echo "history + schema unchanged"; else status=1; fi
if diff "$OUT/accessor-old.log.latest-kpi" "$OUT/accessor-new.log.latest-kpi"; then echo "stored data unchanged"; else status=1; fi
grep -hE "DbValidate|DbMigrate|ERROR" "$OUT/accessor-new.log" "$OUT/masterdata-new.log"
exit $status
