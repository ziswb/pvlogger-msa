#!/usr/bin/env bash
set -euo pipefail

# Manual companion step to migration V8__AddBatterySimReadOnlyRole.sql.
# Enables LOGIN for battery_sim_ro and sets its password from 1Password.
#
# Requires: psql, 1Password CLI (op), superuser access to the pvdata database.
#
# Usage:
#   POSTGRESQL_HOST=<db-host> [POSTGRESQL_PORT=5432] [OP_ITEM=<op-uri>] [PVLOGGER_OP_ACCOUNT=<account-url>] ./set-battery-sim-ro-password.sh

HOST="${POSTGRESQL_HOST:?set POSTGRESQL_HOST to the PostgreSQL server}"
PORT="${POSTGRESQL_PORT:-5432}"
OP_ITEM="${OP_ITEM:-op://Private/PVLogger battery_sim_ro/password}"
# The item lives in the personal account. Deliberately NOT named OP_ACCOUNT:
# the shell exports OP_ACCOUNT=<work-op-account> (work default), which would
# silently win over any fallback here.
PVLOGGER_OP_ACCOUNT="${PVLOGGER_OP_ACCOUNT:-my.1password.com}"

PASSWORD="$(op read --account "$PVLOGGER_OP_ACCOUNT" "$OP_ITEM")"

# :'pw' lets psql quote the password safely, whatever characters it contains
echo "ALTER ROLE battery_sim_ro LOGIN PASSWORD :'pw';" |
    psql -h "$HOST" -p "$PORT" -U postgres -d pvdata -v ON_ERROR_STOP=1 -v pw="$PASSWORD"

echo "battery_sim_ro login enabled, password set from '$OP_ITEM'"
