#!/usr/bin/env bash
#
# Script: screenshots.sh <outdir>
# Purpose: Run the SPA via `ng serve` (proxy.conf.json: /v1 -> :8083, /livedata -> :8085) against
#          the real data accessor + frontend Ktor and a throwaway PostgreSQL 18 seeded with
#          "today" data, then take desktop/phone screenshots, dump the rendered DOM and run
#          live-check.mjs (WebSocket card update, day navigation) in a real Chrome.
#          The dashboard always opens on today, so compare captures taken on the same day.
#          Needs ports 8083, 8085 and 4300 free (stop locally running services).
# Dependencies: docker, java (toolchain JDK), node/npm, python3, curl, Google Chrome
#               (override the binary with CHROME=...)
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/lib.sh"
OUT=${1:?usage: screenshots.sh <outdir>}
mkdir -p "$OUT"; OUT=$(cd "$OUT" && pwd)
WEB=${WEB:-$REPO/pvlogger-frontend/src/web}
CHROME=${CHROME:-"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"}
rm -rf "$OUT/screens"; S="$OUT/screens"; mkdir -p "$S/logs"
stop_all() { pkill -f "ng serve --port 4300" 2>/dev/null || true; cleanup; }
trap stop_all EXIT

build_jars
(cd "$WEB" && npm ci > "$S/logs/npm-ci.log" 2>&1)
start_db
start_jar "$(jar_of pvlogger-modbus-data-accessor)" "$S/logs/accessor.log" "${DB_ARGS[@]}" -P:ktor.deployment.port=8083
start_jar "$(jar_of pvlogger-frontend)" "$S/logs/frontend.log" -P:ktor.deployment.port=8085 -P:pvlogger.dataaccessor.port=8083
wait_for http://localhost:8083/metrics && wait_for http://localhost:8085/metrics

# Today 06:00-20:00 every 10 min: a solar bell curve and a varying load.
python3 - <<'PY'
import json, math, urllib.request, datetime, zoneinfo
tz = zoneinfo.ZoneInfo("Europe/Zurich")
d = datetime.datetime.now(tz).replace(hour=6, minute=0, second=0, microsecond=0)
produced = exported = imported = consumed = selfc = 0.0
for i in range(85):
    t = d + datetime.timedelta(minutes=10 * i)
    sun = max(0.0, math.sin(math.pi * i / 84)) * 6000
    load = 600 + 400 * math.sin(i / 7) ** 2
    self_ = min(sun, load); exp = sun - self_; imp = load - self_
    produced += sun / 6; exported += exp / 6; imported += imp / 6; consumed += load / 6; selfc += self_ / 6
    body = {"jobId": f"s{i}", "dateCreated": int(t.timestamp() * 1000),
            "producing": {"w": sun}, "exporting": {"w": exp}, "importing": {"w": imp},
            "consuming": {"w": load}, "selfConsuming": {"w": self_},
            "produced": {"wh": produced}, "exported": {"wh": exported}, "imported": {"wh": imported},
            "consumed": {"wh": consumed}, "selfConsumed": {"wh": selfc}}
    urllib.request.urlopen(urllib.request.Request("http://localhost:8083/v1/store-kpi-values",
        json.dumps(body).encode(), {"Content-Type": "application/json"}))
PY

(cd "$WEB" && npx ng serve --port 4300 > "$S/logs/ngserve.log" 2>&1) &
PIDS+=($!)
wait_for http://localhost:4300/ 180
sleep 3
for size in 1440,2400 390,3200; do
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=$size \
    --virtual-time-budget=20000 --screenshot="$S/dashboard-${size/,/x}.png" http://localhost:4300/ >/dev/null 2>&1
done
"$CHROME" --headless=new --disable-gpu --virtual-time-budget=20000 --dump-dom http://localhost:4300/ 2>/dev/null > "$S/dom.html"
CHROME="$CHROME" node "$CHECK_DIR/live-check.mjs" "$S/logs/chrome-profile" | tee "$S/live-check.txt"
echo "screens: $S"
