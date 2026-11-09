#!/usr/bin/env bash
#
# Script: snapshot.sh <outdir>
# Purpose: Start data accessor, masterdata server, frontend (Ktor) and kpi-exporter (serve)
#          against a throwaway PostgreSQL 18, seed fixed data and record every response,
#          the metric names, the Flyway history and the schema. Two snapshots of the same
#          seed must be identical (compare.sh ignores only the HTTP Date header).
#          Also keeps copies of the service jars in <outdir>/jars for flyway-takeover.sh.
# Dependencies: docker, java (toolchain JDK), go, node, python3, curl
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/lib.sh"
OUT=${1:?usage: snapshot.sh <outdir>}
mkdir -p "$OUT"; OUT=$(cd "$OUT" && pwd)
rm -rf "$OUT/snapshot"; S="$OUT/snapshot"; mkdir -p "$S/logs" "$OUT/jars"
trap cleanup EXIT

build_jars
for m in pvlogger-modbus-data-accessor pvlogger-masterdata-server pvlogger-frontend; do cp "$(jar_of $m)" "$OUT/jars/"; done
(cd "$REPO/pvlogger-kpi-exporter" && go build -o "$S/logs/exporter" ./cmd/exporter)
start_db

start_jar "$(jar_of pvlogger-modbus-data-accessor)" "$S/logs/accessor.log" "${DB_ARGS[@]}" -P:ktor.deployment.port=18083
start_jar "$(jar_of pvlogger-masterdata-server)" "$S/logs/masterdata.log" "${DB_ARGS[@]}" -P:ktor.deployment.port=18081
start_jar "$(jar_of pvlogger-frontend)" "$S/logs/frontend.log" -P:ktor.deployment.port=18085 -P:pvlogger.dataaccessor.port=18083
echo -n "upgrade-check-token" > "$S/logs/token"
# The exporter refuses to format timestamps without TZ (production sets it).
TZ=Europe/Zurich KPI_EXPORTER_MODE=serve KPI_FILE_PREFIX=site HTTP_PORT=18090 REST_PORT=18083 \
  KPI_EXPORT_TOKEN_FILE="$S/logs/token" "$S/logs/exporter" > "$S/logs/exporter.log" 2>&1 &
PIDS+=($!)

for url in localhost:18083/metrics localhost:18081/metrics localhost:18085/metrics localhost:18090/healthz; do
  wait_for "http://$url" || { echo "a service did not start, see $S/logs" >&2; exit 1; }
done

A=http://localhost:18083/v1
# Fixed KPI series on 2026-11-02 (Zurich), every 15 min 06:00-20:00, plus raw meter values.
python3 - "$A" <<'PY'
import json, sys, urllib.request, datetime, zoneinfo
base = sys.argv[1]; tz = zoneinfo.ZoneInfo("Europe/Zurich")
def post(path, body):
    r = urllib.request.Request(base + path, json.dumps(body).encode(), {"Content-Type": "application/json"})
    assert urllib.request.urlopen(r).status == 200
t = datetime.datetime(2026, 11, 2, 6, 0, tzinfo=tz); i = 0
while t.hour < 20:
    p = 100.0 * i
    post("/store-kpi-values", {"jobId": f"snap-{i}", "dateCreated": int(t.timestamp() * 1000),
        "producing": {"w": p}, "exporting": {"w": p / 3}, "importing": {"w": 50.0 + i},
        "consuming": {"w": 400.0 + i}, "selfConsuming": {"w": p * 2 / 3},
        "produced": {"wh": 25.0 * i}, "exported": {"wh": 8.0 * i}, "imported": {"wh": 12.5 * i},
        "consumed": {"wh": 100.0 * i}, "selfConsumed": {"wh": 17.0 * i}})
    t += datetime.timedelta(minutes=15); i += 1
for i, (iy, me, mi) in enumerate([(1000.5, 200.0, 300.0), (1003.25, 201.5, 300.75), (1010.0, 205.0, 302.0)]):
    post("/store-raw-kpi-register-values", {"jobId": f"raw-{i}", "inverterActivePower": 1500.0 + i,
        "meterActivePower": -300.0 + i, "inverterDailyYield": 3.5 + i, "inverterAccumulatedYield": iy,
        "meterExportedEnergy": me, "meterImportedEnergy": mi})
PY

# req <name> <curl args...>: status line (code + content type) and body.
req() { local n=$1; shift; curl -s -o "$S/$n.body" -w '%{http_code} %{content_type}\n' "$@" > "$S/$n.status" || true; }
req daydata-date "$A/daydata/2026-11-2"
req daydata-empty-date "$A/daydata/2020-1-1"
req daydata-bad-date "$A/daydata/not-a-date"
req latest-kpi "$A/read-latest-kpi"
req meter-energy-today "$A/meter-energy-today"
req store-kpi-malformed -X POST -H 'Content-Type: application/json' -d '{not valid json}' "$A/store-kpi-values"
req store-kpi-missing-fields -X POST -H 'Content-Type: application/json' -d '{"jobId":"x"}' "$A/store-kpi-values"
req accessor-unknown-path "$A/does-not-exist"
req masterdata-list "http://localhost:18081/master-data"
req masterdata-address "http://localhost:18081/master-data/32080"
req frontend-root "http://localhost:18085/"
req frontend-unknown "http://localhost:18085/v1/nothing"
req export-no-token "http://localhost:18090/v1/export/latest"
req export-bad-token -H 'Authorization: Bearer wrong' "http://localhost:18090/v1/export/latest"
req export-latest -D "$S/export-latest.headers" -H 'Authorization: Bearer upgrade-check-token' "http://localhost:18090/v1/export/latest"
req exporter-health "http://localhost:18090/healthz"

# Metric names and types, no values (dashboards and alerts depend on the names).
for s in accessor:18083 masterdata:18081 frontend:18085; do
  curl -s "http://localhost:${s#*:}/metrics" | grep -E '^# TYPE' | sort > "$S/metrics-${s%%:*}.txt" || true
done

# First /livedata WebSocket frame.
node -e '
const ws = new WebSocket("ws://localhost:18085/livedata");
const t = setTimeout(() => { console.log("TIMEOUT"); process.exit(0); }, 75000);
ws.onmessage = m => { console.log(m.data); clearTimeout(t); ws.close(); process.exit(0); };
ws.onerror = e => { console.log("ERROR " + e.message); process.exit(0); };
' > "$S/livedata.body"

for db in pvdata pvmasterdata; do
  docker exec "$DB_CONTAINER" psql -U postgres -d $db -Atc \
    "select version, description, success from flyway_schema_history order by installed_rank" > "$S/flyway-$db.txt"
  docker exec "$DB_CONTAINER" pg_dump -U postgres -s -d $db | grep -vE '^--|^$|^SET |restrict ' > "$S/schema-$db.sql"
done
# For reading, not compared: unexpected errors and JVM warnings in the service logs.
grep -hiE 'error|exception|^WARNING' "$S"/logs/*.log | grep -vE 'StatusPages|BadRequest|JsonConvert|JsonDecoding|MissingField|DateTimeParse|already exists, skipping|^\s+at ' \
  > "$OUT/snapshot-log-findings.txt" || true
echo "snapshot: $S"
