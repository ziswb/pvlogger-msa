#!/usr/bin/env bash
#
# Shared helpers for the upgrade-check scripts (sourced, not run).
# Runs everything against a throwaway PostgreSQL container; never touches the
# local dev database or anything in the cluster.

REPO=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
CHECK_DIR="$REPO/scripts/upgrade-check"
OUT_ROOT="$REPO/build/upgrade-check"
DB_CONTAINER=pvlogger-upgrade-check-db
DB_PORT=55432
DB_PASSWORD=upgrade-check
DB_ARGS=(-P:database.dbServerUrl=jdbc:postgresql://localhost:$DB_PORT/ -P:database.password=$DB_PASSWORD)

# The JDK the Gradle toolchain targets (macOS java_home), so the jars run on what production runs.
jdk_version() { grep -ho 'JavaLanguageVersion.of([0-9]*)' "$REPO/pvlogger-shared-model/build.gradle.kts" | grep -o '[0-9]\+'; }
export JAVA_HOME=${JAVA_HOME_OVERRIDE:-$(/usr/libexec/java_home -v "$(jdk_version)")}

PIDS=()
cleanup() {
  for p in "${PIDS[@]:-}"; do [ -n "$p" ] && kill "$p" 2>/dev/null || true; done
  for p in "${PIDS[@]:-}"; do [ -n "$p" ] && wait "$p" 2>/dev/null || true; done
  PIDS=()
  docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
}

# Fresh PostgreSQL in the production major version, Zurich time like the cluster.
start_db() {
  docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
  docker run -d --name "$DB_CONTAINER" -e POSTGRES_PASSWORD=$DB_PASSWORD -e TZ=Europe/Zurich \
    -p $DB_PORT:5432 postgres:18 >/dev/null
  until docker exec "$DB_CONTAINER" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done
  sleep 2
}

build_jars() {
  (cd "$REPO" && ./gradlew -q :pvlogger-modbus-data-accessor:buildFatJar :pvlogger-frontend:buildFatJar :pvlogger-masterdata-server:buildFatJar)
}

# start_jar <jar> <log> <ktor args...>: runs a fat jar with its dev profile in the background.
start_jar() {
  local jar=$1 log=$2; shift 2
  java -jar "$jar" -config=application-dev.conf "$@" > "$log" 2>&1 &
  PIDS+=($!)
}

# wait_for <url> [seconds]: returns non-zero if the URL never answers.
wait_for() {
  for _ in $(seq 1 "${2:-90}"); do curl -sf -o /dev/null "$1" && return 0; sleep 1; done
  echo "not answering: $1" >&2; return 1
}

jar_of() { echo "$REPO/$1/build/libs/$1-all.jar"; }
