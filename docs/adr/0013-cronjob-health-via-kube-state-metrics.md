# 0013 — CronJob health via kube-state-metrics; Pushgateway carries domain KPIs only

- Status: accepted
- Date: 2026-08-12

## Context

Consolidation backlog item 11: only the fast and full collectors push
Pushgateway metrics; the device collector, fusionsolar(-auth) and the
kpi-exporter push nothing and appeared to "fail silently". Decide: metrics
everywhere or an explicit reason why not.

Two observations settle it. First, what the shared
`pvlogger-shared-modbus/metrics` package pushes is *domain* data — ten solar
KPI gauges (`pvlogger_solar_*`) — which has no analogue in the other services
(identifiers, third-party API sync, file export). Second, the failures are not
actually silent at the Kubernetes level: every Go CronJob exits non-zero on
error (`Fatal` in `cmd/` and job dispatch, ADR-0010), and the infrastructure
repo already runs kube-state-metrics scraped by Prometheus, which exposes
`kube_job_status_failed`, `kube_cronjob_status_last_successful_time` and
`kube_cronjob_spec_suspend` for every CronJob — including fusionsolar-auth and
the DB backup, which item 11 didn't even list. A per-service "job succeeded"
pusher would duplicate that signal and spread `PUSHGATEWAY_*` config to four
more services.

## Decision

**Job health comes from kube-state-metrics, not from the services.**
PrometheusRules in the infrastructure repo (`roles/prometheus`) alert on
failed and stale CronJobs; the alerts are displayed on the pvlogger Grafana
dashboard via the `ALERTS` series (no notification channel — a deliberate
choice, revisit separately). **The Pushgateway is reserved for domain metrics**
that have no Kubernetes-level equivalent — currently the solar KPI gauges of
the active modbus collector. Services without domain metrics push nothing,
by design.

## Consequences

- No new code in any collector; a future CronJob is covered by the generic
  failure/staleness alerts automatically (an unusual schedule may need its
  own threshold tier in the rules).
- Failure visibility hinges on exit codes: library code must keep returning
  errors so mains exit non-zero (ADR-0010).
- Alert rules and dashboard live in the infrastructure repo
  (`roles/prometheus`, `roles/grafana_install`), not here.
- Fast and full collectors share one Pushgateway job name
  (`pvlogger_modbus_data_collector`) — acceptable while they are mutually
  exclusive (ADR-0004); revisit if that toggle ever goes away.
