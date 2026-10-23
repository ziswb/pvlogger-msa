# PostgreSQL Migration Guide: PG14 → PG18 with Logical Replication

**Scenario:** 7 databases, two PostgreSQL servers running on `<central-host>`, workload in Kubernetes.

| | Old Server | New Server |
|---|---|---|
| **Image** | `postgres:14-vectorchord0.4.3-pgvectors0.2.0` | `postgres:18-vectorchord0.5.3-pgvector0.8.1` |
| **Port** | `5433` | `5434` |
| **Host** | `<central-host>` | `<central-host>` |
| **Superuser** | `postgres` | `postgres` |

**Databases to migrate:**
- `fsdata`, `heartbeat`, `immich`, `nextcloud`, `pvdata`, `pvmasterdata`, `sunrisesunset`

> **Note on extensions:** Extensions are only installed in the `immich` database. The other six databases use no extensions beyond the built-in `plpgsql`. The `immich` database has the following extensions installed on PG14:
>
> | Extension | Version | Description |
> |---|---|---|
> | `cube` | 1.5 | Multidimensional cube data type |
> | `earthdistance` | 1.1 | Great-circle distance calculations |
> | `pg_trgm` | 1.6 | Trigram-based text similarity and indexing |
> | `plpgsql` | 1.0 | PL/pgSQL procedural language (built-in) |
> | `unaccent` | 1.1 | Text search dictionary removing accents |
> | `uuid-ossp` | 1.1 | UUID generation |
> | `vchord` | 0.4.3 | Vector database plugin (Rust, LLM-optimised) |
> | `vector` | 0.8.0 | pgvector: vector data type, IVFFlat and HNSW indexes |

**Kubernetes workloads:**
- Namespace `pvlogger`: `pvlogger` (Helm-managed)
- Namespace `immich`: `immich-machine-learning`, `immich-server`

---

## Overview & Timeline

```
PHASE 1: Prepare PG14           (~10 min)
PHASE 2: Prepare PG18           (~15 min)
PHASE 3: Schema migration        (~5 min)
PHASE 4: Start replication       (~minutes to hours depending on data volume)
PHASE 5: Monitor sync            (ongoing until caught up)
PHASE 6: Cutover                 (~5 min downtime)
PHASE 7: Verify & cleanup        (~10 min)
```

---

## Conventions

All `psql` commands in this guide use explicit host/port flags. Commands starting with `## [PG14]` run against the old server; `## [PG18]` run against the new server.

Define these shell variables once at the start of your session to make copy-paste easier:

```bash
export PG14_HOST="<central-host>"
export PG14_PORT="5433"
export PG18_HOST="<central-host>"
export PG18_PORT="5434"
export PG_USER="postgres"
DATABASES=("fsdata" "heartbeat" "immich" "nextcloud" "pvdata" "pvmasterdata" "sunrisesunset")
```

---

## PHASE 1: Prepare PG14

### 1.1 — Verify PG14 is reachable and check version

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres -c "SELECT version();"
```

**Expected:** A line containing `PostgreSQL 14.x`.

---

### 1.2 — Check current `wal_level`

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres -c "SHOW wal_level;"
```

**Expected output:**
```
 wal_level
-----------
 logical
```

If the result is `replica` or `minimal`, you must change it:

> **If wal_level is NOT `logical`:**
> 1. Edit `postgresql.conf` inside the PG14 container and set `wal_level = logical`
> 2. Restart the PG14 container: `docker compose restart <pg14-service-name>`
> 3. Re-run the check above before continuing

---

### 1.3 — Check and set `max_wal_senders`, `max_replication_slots`, and `wal_keep_size`

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SHOW max_wal_senders; SHOW max_replication_slots; SHOW wal_keep_size;"
```

**Required values:**

| Setting | Minimum | Recommended | Reason |
|---|---|---|---|
| `max_wal_senders` | 64 | 64 | One per subscription plus one per table sync worker — with many tables syncing in parallel across 7 databases this is easily exhausted at the default of 10 |
| `max_replication_slots` | 64 | 64 | 7 permanent slots (one per subscription) plus temporary per-table slots created during initial sync — these can be numerous with many tables syncing in parallel |
| `wal_keep_size` | 1GB | 1GB | Retain WAL in case a subscriber falls behind |

Set all three via `ALTER SYSTEM` and restart:

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres <<'EOF'
ALTER SYSTEM SET max_wal_senders = 64;
ALTER SYSTEM SET max_replication_slots = 64;
ALTER SYSTEM SET wal_keep_size = '1GB';
EOF
```

```bash
docker compose restart <pg14-service-name>
```

**Verify after restart:**

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SHOW max_wal_senders; SHOW max_replication_slots; SHOW wal_keep_size;"
```

---

### 1.4 — Create the replication user on PG14

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres <<'EOF'
CREATE USER replicator WITH REPLICATION LOGIN PASSWORD '<replication-password>';
EOF
```

**Verify the user was created:**

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT usename, userepl FROM pg_user WHERE usename = 'replicator';"
```

**Expected:**
```
  usename   | userepl
------------+---------
 replicator | t
```

---

### 1.5 — Grant SELECT on all tables to replicator (per database)

Run this loop to grant the replicator user read access on all tables in every database:

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Granting CONNECT and SELECT on $db ..."
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
    -c "GRANT CONNECT ON DATABASE ${db} TO replicator;"
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" <<EOF
GRANT SELECT ON ALL TABLES IN SCHEMA public TO replicator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO replicator;
EOF
done
```

**Verify (spot-check one database, e.g. fsdata):**

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT has_database_privilege('replicator', 'fsdata', 'CONNECT');"

psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d fsdata \
  -c "SELECT grantee, privilege_type FROM information_schema.role_table_grants WHERE grantee = 'replicator' LIMIT 5;"
```

**Expected:** `has_database_privilege` returns `t`, and the second query returns rows showing `replicator` with `SELECT` privilege.

---

### 1.6 — Ensure all tables have PRIMARY KEYS or REPLICA IDENTITY

Logical replication requires each table to have a `PRIMARY KEY` or `REPLICA IDENTITY FULL` set to replicate `UPDATE` and `DELETE` operations.

Check for tables missing a primary key in each database:

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Checking $db for tables without primary keys ..."
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" <<EOF
SELECT schemaname, tablename
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT IN (
    SELECT t.table_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.tables t ON tc.table_name = t.table_name
    WHERE tc.constraint_type = 'PRIMARY KEY'
      AND t.table_schema = 'public'
  );
EOF
done
```

**Expected:** No rows returned for any database. If tables are listed, for each one either:

```sql
-- Option A: Add a primary key (preferred)
ALTER TABLE <tablename> ADD PRIMARY KEY (<column>);

-- Option B: Set REPLICA IDENTITY FULL (replicates entire row; slower but no schema change)
ALTER TABLE <tablename> REPLICA IDENTITY FULL;
```

---

### 1.7 — Create publications on PG14 (one per database)

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Creating publication on $db ..."
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" \
    -c "CREATE PUBLICATION upgrade_pub FOR ALL TABLES;"
done
```

**Verify all publications were created:**

```bash
for db in "${DATABASES[@]}"; do
  echo "==> $db:"
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" \
    -c "SELECT pubname, puballtables FROM pg_publication;"
done
```

**Expected for each database:**
```
  pubname    | puballtables
-------------+--------------
 upgrade_pub | t
```

---

### 1.8 — Allow replicator connections in pg_hba.conf on PG14

Ensure the PG14 `pg_hba.conf` allows the replicator user to connect for replication and normal access. Add these lines if not present (the PG18 container will connect from the same host):

```
# Allow replicator for logical replication
host    all             replicator      127.0.0.1/32            md5
host    replication     replicator      127.0.0.1/32            md5
```

If the PG18 container has a different IP than `127.0.0.1`, use that IP or `0.0.0.0/0` (restrict in production). After editing, reload:

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres -c "SELECT pg_reload_conf();"
```

**Verify:**

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT pg_reload_conf();" && echo "pg_hba.conf reloaded OK"
```

---

## PHASE 2: Prepare PG18

### 2.1 — Verify PG18 is reachable and check version

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres -c "SELECT version();"
```

**Expected:** A line containing `PostgreSQL 18.x`.

---

### 2.2 — Create all 7 databases on PG18

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Creating database $db on PG18 ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
    -c "CREATE DATABASE $db;"
done
```

**Verify all databases exist:**

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT datname FROM pg_database WHERE datname IN ('fsdata','heartbeat','immich','nextcloud','pvdata','pvmasterdata','sunrisesunset') ORDER BY datname;"
```

**Expected:** All 7 database names listed.

---

### 2.3 — Create the replication user on PG18

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres <<'EOF'
CREATE USER replicator WITH REPLICATION LOGIN PASSWORD '<replication-password>';
EOF
```

**Verify:**

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT usename, userepl FROM pg_user WHERE usename = 'replicator';"
```

---

### 2.4 — Configure logical replication workers on PG18

With 7 subscriptions, each needing at least one worker for the initial table copy plus one for streaming, the default `max_logical_replication_workers` of 4 is insufficient. Raise it along with `max_worker_processes` (the pool from which logical replication workers are allocated) and `max_active_replication_origins` (one origin is tracked per table sync worker on PG18 — with many tables syncing in parallel this limit is easily exhausted).

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SHOW max_worker_processes; SHOW max_logical_replication_workers; SHOW max_active_replication_origins;"
```

Then set the required values:

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres <<'EOF'
ALTER SYSTEM SET max_logical_replication_workers = 16;
ALTER SYSTEM SET max_worker_processes = 32;
ALTER SYSTEM SET max_active_replication_origins = 64;
EOF
```

All three settings require a **full PostgreSQL restart** to take effect:

```bash
docker compose restart <pg18-service-name>
```

**Verify after restart:**

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SHOW max_worker_processes; SHOW max_logical_replication_workers; SHOW max_active_replication_origins;"
```

**Expected:**
```
 max_worker_processes
----------------------
 32

 max_logical_replication_workers
---------------------------------
 16

 max_active_replication_origins
--------------------------------
 64
```

---

### 2.5 — Verify extensions are available on PG18

Only the `immich` database requires extensions. Check that all eight extensions used by `immich` on PG14 are available on PG18. The standard PostgreSQL extensions (`cube`, `earthdistance`, `pg_trgm`, `plpgsql`, `unaccent`, `uuid-ossp`) ship with PostgreSQL itself and will always be present. The two that need explicit verification are `vector` (pgvector) and `vchord` (vectorchord), as they are image-specific:

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT name, default_version FROM pg_available_extensions WHERE name IN ('vector','vchord') ORDER BY name;"
```

**Expected:**
```
  name  | default_version
--------+-----------------
 vchord | 0.5.3
 vector | 0.8.1
```

If either extension is missing, the PG18 image is not correctly configured. Do not proceed until both are listed.

---

## PHASE 3: Schema Migration (PG14 → PG18)

This phase dumps only the schema (no data) from PG14 and loads it into PG18. This must be done **before** creating subscriptions.

### 3.1 — Dump and restore schemas for all databases

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Migrating schema for $db ..."
  pg_dump \
    -h $PG14_HOST -p $PG14_PORT -U $PG_USER \
    --schema-only \
    --no-owner \
    --no-acl \
    -d "$db" \
  | psql \
    -h $PG18_HOST -p $PG18_PORT -U $PG_USER \
    -d "$db"
  echo "==> Schema migration for $db: done"
done
```

> **Note on the `immich` database:** The schema dump will include `CREATE EXTENSION` statements for all eight extensions. The standard PostgreSQL extensions (`cube`, `earthdistance`, `pg_trgm`, `plpgsql`, `unaccent`, `uuid-ossp`) will restore without issue. The `vchord` extension name is the same on both images, so no manual editing should be required. If you nonetheless see extension-related errors during the restore of `immich`, dump its schema to a file first so you can inspect and edit it:
>
> ```bash
> pg_dump -h $PG14_HOST -p $PG14_PORT -U $PG_USER --schema-only --no-owner --no-acl -d immich > /tmp/immich_schema.sql
> # Inspect CREATE EXTENSION lines:
> grep "CREATE EXTENSION" /tmp/immich_schema.sql
> # Edit if necessary, then restore:
> psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d immich < /tmp/immich_schema.sql
> ```

---

### 3.2 — Verify schemas were created on PG18

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Tables in $db on PG18:"
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"
done
```

**Expected:** Each database should list the same tables as on PG14. Spot-check by running the same query against PG14 and comparing:

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Tables in $db on PG14:"
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" \
    -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"
done
```

---

### 3.3 — Grant privileges on PG18 schemas to replicator

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Granting privileges in $db on PG18 ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" <<EOF
GRANT ALL ON ALL TABLES IN SCHEMA public TO replicator;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO replicator;
GRANT ALL ON SCHEMA public TO replicator;
EOF
done
```

---

## PHASE 4: Start Logical Replication

### 4.1 — Create subscriptions on PG18 (one per database)

This command starts the initial data sync (bulk copy) from PG14 into PG18, then switches to streaming replication.

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Creating subscription for $db on PG18 ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" <<EOF
CREATE SUBSCRIPTION upgrade_sub_${db}
  CONNECTION 'host=${PG14_HOST} port=${PG14_PORT} dbname=${db} user=replicator password=<replication-password> sslmode=prefer'
  PUBLICATION upgrade_pub;
EOF
done
```

**Verify subscriptions were created:**

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT subname, subenabled FROM pg_subscription ORDER BY subname;"
```

> **Note:** `pg_subscription` is a cluster-level catalog shared across all databases. Querying it from any database returns all 7 subscriptions — this is expected and correct.

**Expected:** All 7 subscriptions listed, each with `subenabled = t`.

---

### 4.2 — Verify replication slots were created on PG14

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT slot_name, slot_type, active, restart_lsn FROM pg_replication_slots ORDER BY slot_name;"
```

**Expected:** 7 slots (one per database), all with `active = true` and `slot_type = logical`.

---

## PHASE 5: Monitor Synchronisation

Run these monitoring commands repeatedly until all tables in all databases are in the `ready` state.

### 5.1 — Monitor per-table sync state (run repeatedly)

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Sync state for $db:"
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" <<EOF
SELECT
  srsubid,
  srrelid::regclass AS table_name,
  CASE srsubstate
    WHEN 'i' THEN 'initializing'
    WHEN 'd' THEN 'data-copy (in progress)'
    WHEN 'f' THEN 'finished copy'
    WHEN 's' THEN 'synced'
    WHEN 'r' THEN 'READY ✓'
    ELSE srsubstate::text
  END AS state
FROM pg_subscription_rel
ORDER BY state, table_name;
EOF
done
```

**All tables should eventually reach `READY ✓` before cutover.**

---

### 5.2 — Monitor replication lag (run repeatedly)

Check how far behind PG18 is from PG14:

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres <<'EOF'
SELECT
  slot_name,
  active,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) AS replication_lag,
  confirmed_flush_lsn
FROM pg_replication_slots
WHERE slot_type = 'logical'
ORDER BY slot_name;
EOF
```

**Expected at cutover:** All slots show lag of `0 bytes` or a few bytes.

---

### 5.3 — Monitor subscription receive state on PG18

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT subname, received_lsn, latest_end_lsn, latest_end_time FROM pg_stat_subscription ORDER BY subname;"
```

> **Note:** Like `pg_subscription`, `pg_stat_subscription` is a cluster-level view — a single query from any database returns the state of all 7 subscriptions.

**Expected:** `received_lsn` advancing and `latest_end_time` showing recent timestamps for subscriptions that have completed their initial table copy. Subscriptions still in the bulk copy phase will show empty `received_lsn` and `latest_end_time` — this is normal. Use step 5.1 to check per-table copy progress for those.

---

### 5.4 — Quick overall readiness check

Use this single command to get a summary of all databases:

```bash
echo "=== READINESS SUMMARY ===" && \
for db in "${DATABASES[@]}"; do
  total=$(psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_subscription_rel;")
  ready=$(psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" -tAc "SELECT COUNT(*) FROM pg_subscription_rel WHERE srsubstate = 'r';")
  lag=$(psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres -tAc "SELECT pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) FROM pg_replication_slots WHERE slot_name LIKE '%${db}%' LIMIT 1;")
  echo "  $db: $ready/$total tables ready | lag: $lag"
done
```

**Proceed to cutover only when all databases show all tables ready and lag near 0.**

---

## PHASE 6: Cutover

> ⚠️ **This phase causes downtime. Perform during a low-traffic window.**
> Expected downtime: 1–5 minutes.

### 6.1 — Scale down Kubernetes workloads

```bash
1. Suspend all PVLogger CronJobs                                                                                                                         
                                                                                                                                                       
# Suspend every CronJob in the pvlogger namespace                                                                                                        
kubectl get cronjob -n pvlogger -o name \                                                                                                                
| xargs -I {} kubectl patch -n pvlogger {} -p '{"spec":{"suspend":true}}'                                                                              
   
# Verify                                                                                                                                                 
kubectl get cronjob -n pvlogger                                                                                                                          
                                                                                                                                                       
2. Wait for any in-flight CronJob pods to finish                                                                                                         
                                                                                                                                                       
kubectl get jobs -n pvlogger                                                                                                                             
# If any are still active, wait or delete them:                                                                                                          
# kubectl delete job -n pvlogger <job-name>                                                                                                              
                                                                                                                                                       
3. Scale down the DB-using Deployments                                                                                                                   
                                                                                                                                                       
# Immich                                                                                                                                                 
kubectl scale deploy -n immich immich-server --replicas=0                                                                                              
kubectl scale deploy -n immich immich-machine-learning --replicas=0                                                                                      
   
# PVLogger                                                                                                                                               
kubectl scale deploy -n pvlogger pvlogger-masterdata-server --replicas=0                                                                               
kubectl scale deploy -n pvlogger pvlogger-modbus-data-accessor --replicas=0                                                                              
kubectl scale deploy -n pvlogger pvlogger-frontend --replicas=0                                                                                          
                                                                                                                                                       
Heads up: re-running ansible-playbook playbooks/03-workloads.yml or the pvlogger Helm deploy will undo the suspend/scale-down, so do the maintenance     
before the next deploy.                                                                                                                                
```

**Verify all pods are terminated:**

```bash
kubectl get pods -n pvlogger
kubectl get pods -n immich
```

**Expected:** No running pods in either namespace (`No resources found` or all pods `Terminating` → gone).

---

### 6.2 — Wait for replication lag to reach zero

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres <<'EOF'
SELECT
  slot_name,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) AS replication_lag
FROM pg_replication_slots
WHERE slot_type = 'logical'
ORDER BY slot_name;
EOF
```

**Required:** All slots show `0 bytes`. Re-run every 5 seconds until confirmed. Do not proceed until lag is zero.

---

### 6.3 — Record the final LSN on PG14 (safety checkpoint)

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT pg_current_wal_lsn() AS final_lsn_pg14;"
```

Note this value. You will verify PG18 has received up to this point.

---

### 6.4 — Verify PG18 has received all WAL up to that LSN

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "SELECT subname, received_lsn FROM pg_stat_subscription ORDER BY subname;"
```

The `received_lsn` values should be at or beyond the `final_lsn_pg14` value noted above.

---

### 6.5 — Disable all subscriptions on PG18

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Disabling subscription for $db ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "ALTER SUBSCRIPTION upgrade_sub_${db} DISABLE;"
done
```

**Verify all subscriptions are disabled:**

```bash
for db in "${DATABASES[@]}"; do
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "SELECT subname, subenabled FROM pg_subscription;"
done
```

**Expected:** `subenabled = false` for every subscription.

---

### 6.6 — Synchronise sequences from PG14 to PG18

> ⚠️ Logical replication does **not** replicate sequences. This is critical — without this step, new rows inserted on PG18 will get duplicate IDs.

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Syncing sequences for $db ..."
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" -tAc "
    SELECT 'SELECT setval(' || quote_literal(schemaname || '.' || sequencename) ||
           ', ' || last_value || ');'
    FROM pg_sequences
    WHERE schemaname = 'public'
      AND last_value IS NOT NULL;
  " | psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db"
  echo "==> Sequences synced for $db"
done
```

> **Note:** `setval(name, n)` with two arguments sets the value and marks `is_called = true`, meaning the next `nextval()` call will return `n + 1`. This is the correct safe behaviour since on PG14 the sequence has already advanced to `last_value`. Sequences where `last_value IS NULL` have never been used and are skipped.

**Verify (spot-check):**

```bash
# Compare sequences between PG14 and PG18 for one database
echo "--- PG14 sequences (fsdata) ---"
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d fsdata \
  -c "SELECT sequencename, last_value FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename;"

echo "--- PG18 sequences (fsdata) ---"
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d fsdata \
  -c "SELECT sequencename, last_value FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename;"
```

Values should match (or PG18 should be equal/higher due to timing).

---

### 6.7 — Quick row count sanity check

Verify row counts match between PG14 and PG18 for each database:

```bash
for db in "${DATABASES[@]}"; do
  echo "=== Row count comparison for $db ==="
  echo "--- PG14 ---"
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" -c "
    SELECT schemaname, tablename,
           (xpath('/row/cnt/text()', query_to_xml('SELECT COUNT(*) AS cnt FROM ' || schemaname || '.' || tablename, false, true, '')))[1]::text::int AS row_count
    FROM pg_tables WHERE schemaname = 'public'
    ORDER BY tablename;
  "
  echo "--- PG18 ---"
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" -c "
    SELECT schemaname, tablename,
           (xpath('/row/cnt/text()', query_to_xml('SELECT COUNT(*) AS cnt FROM ' || schemaname || '.' || tablename, false, true, '')))[1]::text::int AS row_count
    FROM pg_tables WHERE schemaname = 'public'
    ORDER BY tablename;
  "
done
```

**Expected:** Row counts match for all tables in all databases.

---

### 6.8 — Rebuild indexes on PG18 (including vector indexes)

Logical replication only replicates heap data — **indexes are not replicated**. The schema dump in Phase 3 created the index definitions, but they need to be populated. For large vector datasets this can take significant time and should be monitored.

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Reindexing $db on PG18 ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "REINDEX DATABASE $db;"
  echo "==> Reindex of $db: done"
done
```

> **Note:** For the `immich` database, vector indexes (IVFFlat, HNSW) built by `vchord` and `vector` can take significant time to rebuild depending on the number of embeddings stored. You can run `REINDEX DATABASE immich` before cutover while replication is still running to pre-build the indexes — just run it once more after cutover to pick up any remaining changes.

---

### 6.9 — Update Kubernetes workloads to point to PG18

Update the database connection strings for your workloads. The exact method depends on how connection details are stored (ConfigMap, Secret, Helm values). Common approaches:

**Option A — Helm values update (pvlogger):**

```bash
# Get current values
helm get values pvlogger -n pvlogger > /tmp/pvlogger-values-backup.yaml

# Edit and redeploy with new port
# Change the DB port from 5433 to 5434 in your values file, then:
helm upgrade pvlogger <chart> -n pvlogger -f /tmp/pvlogger-new-values.yaml
```

**Option B — Edit Kubernetes Secret directly:**

```bash
# Decode current secret, modify, re-apply
kubectl get secret <db-secret-name> -n immich -o yaml > /tmp/immich-secret-backup.yaml

# Edit the connection string in the secret (base64-encoded values)
# For example, change port 5433 to 5434:
kubectl edit secret <db-secret-name> -n immich
```

**Option C — If using a ConfigMap with a connection string:**

```bash
kubectl edit configmap <db-configmap-name> -n pvlogger
# Change port 5433 → 5434 in the relevant key
```

---

### 6.10 — Scale Kubernetes workloads back up

```bash
# Unsuspend CronJobs
kubectl get cronjob -n pvlogger -o name \
| xargs -I {} kubectl patch -n pvlogger {} -p '{"spec":{"suspend":false}}'
                                                                                                                                                       
# Scale back up (matches current manifests: immich=4, pvlogger=1)                                                                                        
kubectl scale deploy -n immich immich-server --replicas=4                                                                                                
kubectl scale deploy -n immich immich-machine-learning --replicas=4                                                                                      
kubectl scale deploy -n pvlogger pvlogger-masterdata-server --replicas=1                                                                                 
kubectl scale deploy -n pvlogger pvlogger-modbus-data-accessor --replicas=1
kubectl scale deploy -n pvlogger pvlogger-frontend --replicas=1                                                                                                                                                                                                                                               
```

**Verify pods are running:**

```bash
kubectl get pods -n pvlogger
kubectl get pods -n immich
```

**Expected:** All pods in `Running` state.

---

### 6.11 — Verify workloads are healthy and writing to PG18

Check application logs for errors:

```bash
kubectl logs -n pvlogger deployment/pvlogger --tail=50
kubectl logs -n immich deployment/immich-server --tail=50
kubectl logs -n immich deployment/immich-machine-learning --tail=50
```

Verify new writes are arriving on PG18 (spot-check a high-write table; adjust table name as appropriate):

```bash
# Check pvdata on PG18 — compare row count now vs 1 minute ago
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d pvdata \
  -c "SELECT COUNT(*), MAX(created_at) FROM <your_main_table>;"
# Wait 60 seconds, run again — count should increase
```

---

## PHASE 7: Cleanup

Only perform cleanup after you have confirmed the workloads are operating correctly on PG18 for a period of time (recommended: at least 1 hour of normal operation).

### 7.1 — Drop subscriptions on PG18

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Dropping subscription for $db on PG18 ..."
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "DROP SUBSCRIPTION upgrade_sub_${db};"
done
```

**Verify:**

```bash
for db in "${DATABASES[@]}"; do
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "SELECT subname FROM pg_subscription;"
done
```

**Expected:** No rows returned for any database.

---

### 7.2 — Drop publications on PG14

```bash
for db in "${DATABASES[@]}"; do
  echo "==> Dropping publication for $db on PG14 ..."
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" \
    -c "DROP PUBLICATION upgrade_pub;"
done
```

**Verify:**

```bash
for db in "${DATABASES[@]}"; do
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" \
    -c "SELECT pubname FROM pg_publication;"
done
```

**Expected:** No rows returned.

---

### 7.3 — Verify replication slots are gone on PG14

Dropped subscriptions automatically drop their corresponding replication slots. Verify:

```bash
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT slot_name, active FROM pg_replication_slots WHERE slot_type = 'logical';"
```

**Expected:** No rows returned. If any slots remain (e.g. due to an error), drop them manually:

```bash
# Only if slots remain after dropping subscriptions:
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT pg_drop_replication_slot(slot_name) FROM pg_replication_slots WHERE slot_type = 'logical';"
```

---

### 7.4 — Drop replicator user (optional)

On PG14:
```bash
# Revoke database-level CONNECT grants
for db in "${DATABASES[@]}"; do
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
    -c "REVOKE CONNECT ON DATABASE ${db} FROM replicator;"
done

# Revoke table-level SELECT grants
for db in "${DATABASES[@]}"; do
  psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d "$db" <<EOF
REVOKE SELECT ON ALL TABLES IN SCHEMA public FROM replicator;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE SELECT ON TABLES FROM replicator;
EOF
done

# Now drop the user
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "DROP USER IF EXISTS replicator;"
```

On PG18:
```bash
# Revoke all grants
for db in "${DATABASES[@]}"; do
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" <<EOF
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM replicator;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM replicator;
REVOKE ALL ON SCHEMA public FROM replicator;
EOF
done

# Now drop the user
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres \
  -c "DROP USER IF EXISTS replicator;"
```

---

### 7.5 — Final verification on PG18

Run a final health check on PG18:

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d postgres -c "SELECT version();"

for db in "${DATABASES[@]}"; do
  echo "==> Final check for $db:"
  psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d "$db" \
    -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;"
done
```

---

## Rollback Plan

If issues are discovered after cutover but before cleanup, rollback is straightforward:

```bash
# 1. Scale down workloads
kubectl scale deployment pvlogger -n pvlogger --replicas=0
kubectl scale deployment immich-server -n immich --replicas=0
kubectl scale deployment immich-machine-learning -n immich --replicas=0

# 2. Revert Kubernetes config to point back to port 5433 (PG14)
#    (reverse of step 6.9)

# 3. Scale workloads back up
kubectl scale deployment pvlogger -n pvlogger --replicas=<count>
kubectl scale deployment immich-server -n immich --replicas=<count>
kubectl scale deployment immich-machine-learning -n immich --replicas=<count>
```

> ⚠️ Note: Any writes made to PG18 after cutover will **not** be replicated back to PG14. If rollback is needed after significant writes have occurred on PG18, you will need to assess data loss risk.

---

## Troubleshooting

### Subscription fails to create

```bash
# Check PG14 logs for connection refusal
docker compose logs <pg14-service> | tail -50

# Test that replicator can connect from the PG18 host
psql -h $PG14_HOST -p $PG14_PORT -U replicator -d fsdata -c "SELECT 1;"
```

### Table stuck in 'd' (data-copy) state for a long time

Large tables (especially with vector embeddings) can take a very long time to copy. This is normal. Monitor progress:

```bash
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d <dbname> \
  -c "SELECT * FROM pg_subscription_rel WHERE srsubstate = 'd';"
```

### Replication slot shows large lag / inactive

```bash
# Check if the slot is still active
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d postgres \
  -c "SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS wal_retained FROM pg_replication_slots;"
```

If a slot is inactive for too long, PG14 retains WAL indefinitely — this can fill your disk. If a slot is broken, drop the subscription and recreate it (the initial sync will restart for that database):

```bash
# On PG18 — recreate a broken subscription
psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d <dbname> \
  -c "DROP SUBSCRIPTION upgrade_sub_<dbname>;"
# Then repeat step 4.1 for that specific database only
```

### Vector column type errors during replication

If you see errors related to the `vector` or `vchord` types being incompatible between extension versions (pgvector 0.8.0 → 0.8.1, vchord 0.4.3 → 0.5.3), this affects only the `immich` database. Test the workaround of casting the affected column through `text` in a view and publishing the view instead of the table. This is an advanced scenario; verify on a test environment first.

### Sequences still wrong after cutover

```bash
# Re-run sequence sync for a specific database
psql -h $PG14_HOST -p $PG14_PORT -U $PG_USER -d <dbname> -tAc "
  SELECT 'SELECT setval(' || quote_literal(schemaname || '.' || sequencename) ||
         ', ' || last_value || ');'
  FROM pg_sequences
  WHERE schemaname = 'public'
    AND last_value IS NOT NULL;
" | psql -h $PG18_HOST -p $PG18_PORT -U $PG_USER -d <dbname>
```

---

*Guide generated for migration from `postgres:14-vectorchord0.4.3-pgvectors0.2.0` → `postgres:18-vectorchord0.5.3-pgvector0.8.1` on `<central-host>`.*
