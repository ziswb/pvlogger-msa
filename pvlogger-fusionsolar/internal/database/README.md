# fsdata database access

- **Schema = `migrations/`** (tern, ADR-0022). `NNN_name.sql`, numbered without
  gaps; never edit one that has been applied. `001_baseline.sql` is the schema as
  production had it before migrations existed (all `IF NOT EXISTS`).
- **No transaction:** tern runs each migration in a transaction. A migration that
  cannot (`create index concurrently`) starts with the line
  `---- tern: disable-tx ----`; tern then runs its statements one by one
  (`002_fs_response_fk_indexes.sql`). Index every foreign key column you add.
- **Queries:** `query.fsdata.sql`. After changing either, run `sqlc generate`
  here (`brew install sqlc`); sqlc reads `migrations/` as its schema.
- **Apply:** run the fusionsolar image with `JOB_NAME=MIGRATE` and the usual DB
  env (a manual K8s Job). History lives in `public.tern_schema_version`;
  `flyway_schema_history` is a Kotlin-era leftover and unrelated.
