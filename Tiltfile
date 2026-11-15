# -*- mode: Python -*-
#
# Local development environment (backlog item 23): the Helm chart
# (deploy/helm/pvlogger, ADR-0024) with deploy/dev/values-dev.yaml, every image
# built from this checkout, a plain PostgreSQL, the synthetic plant as the
# Modbus device and a 30-day history backfill (ADR-0025).
#
#   tilt up --context rancher-desktop
#   tilt up --context <ctx> -- --k8s-context=<ctx> --timezone=Europe/Vienna
#   tilt down --context rancher-desktop
#
# Dashboard: http://localhost/ (Rancher Desktop's Traefik). See CLAUDE.md,
# "Local Development".

version_settings(constraint='>=0.33.0')

config.define_string('k8s-context', usage='Kubernetes context Tilt may deploy to (default rancher-desktop)')
config.define_string('timezone', usage="IANA zone of the environment (default: the host's zone, else UTC)")
cfg = config.parse()

# Not "pvlogger", the production namespace: should a wrong --k8s-context ever
# get past the guard below, this release cannot overwrite the real one
# (backlog 58).
NAMESPACE = 'pvlogger-dev'

# Guard: only the named local context, never a production cluster.
expected_context = cfg.get('k8s-context', 'rancher-desktop')
allow_k8s_contexts(expected_context)
if k8s_context() != expected_context:
    fail('Current Kubernetes context is "%s", this Tiltfile deploys only to "%s". ' % (k8s_context(), expected_context) +
         'Run `tilt up --context %s`, or pass `-- --k8s-context=<name>` for another local cluster.' % expected_context)


def host_timezone():
    # macOS and most Linux distributions link /etc/localtime into a zoneinfo tree.
    link = str(local('readlink /etc/localtime 2>/dev/null || true', quiet=True, echo_off=True)).strip()
    marker = 'zoneinfo/'
    if marker in link:
        return link[link.index(marker) + len(marker):]
    return 'UTC'


# One zone for everything: CronJob schedules, the plant's day (daily-yield
# reset), the data accessor's "today" and the backfill.
timezone = cfg.get('timezone', host_timezone())
print('pvlogger dev environment: context %s, namespace %s, timezone %s' % (expected_context, NAMESPACE, timezone))


def dev_manifest(path):
    return blob(str(read_file(path)).replace('__NAMESPACE__', NAMESPACE).replace('__TIMEZONE__', timezone))


# --- Images -----------------------------------------------------------------
# Every Dockerfile builds from the repository root. `only` keeps a change in one
# stack from rebuilding the other's images. Repositories match the chart's
# image.repository values (no registry: Rancher Desktop's moby engine shares
# its images with the cluster, nothing is pushed).

# settings.gradle.kts includes every JVM project, so each JVM build needs all of them.
JVM_COMMON = [
    'settings.gradle.kts',
    'build.gradle.kts',
    'gradle',
    'pvlogger-frontend',
    'pvlogger-masterdata-server',
    'pvlogger-modbus-data-accessor',
    'pvlogger-shared-model',
    'pvlogger-shared-rest',
    'pvlogger-shared-database',
    'pvlogger-shared-ktor',
]

# go.work lists every Go module, so the Go builds need all of them.
GO_COMMON = [
    'go.work',
    'go.work.sum',
    'pv-device-data-collector',
    'pv-fast-data-collector',
    'pv-full-data-collector',
    'pv-logger',
    'pvlogger-battery-simulator',
    'pvlogger-fusionsolar',
    'pvlogger-fusionsolar-auth',
    'pvlogger-kpi-exporter',
    'pvlogger-modbus-mock',
    'pvlogger-shared-modbus',
]

JVM_IMAGES = {
    'bziswiler/pvlogger-frontend': 'pvlogger-frontend',
    'bziswiler/pvlogger-modbus-data-accessor': 'pvlogger-modbus-data-accessor',
    'bziswiler/pvlogger-masterdata-server': 'pvlogger-masterdata-server',
}

GO_IMAGES = {
    'bziswiler/pv-fast-data-collector': 'pv-fast-data-collector',
    'bziswiler/pv-full-data-collector': 'pv-full-data-collector',
    'bziswiler/pv-device-data-collector': 'pv-device-data-collector',
    'bziswiler/pvlogger-modbus-mock': 'pvlogger-modbus-mock',
}

# The backends ignore the Angular sources: an SPA edit rebuilds only the frontend.
for repo, module in JVM_IMAGES.items():
    docker_build(repo, '.', dockerfile=module + '/docker/Dockerfile', only=JVM_COMMON,
                 ignore=[] if module == 'pvlogger-frontend' else ['pvlogger-frontend/src/web'])

for repo, module in GO_IMAGES.items():
    docker_build(repo, '.', dockerfile=module + '/docker/Dockerfile', only=GO_COMMON)

# --- Manifests --------------------------------------------------------------

k8s_yaml(blob("""
apiVersion: v1
kind: Namespace
metadata:
  name: %s
""" % NAMESPACE))

k8s_yaml(helm(
    'deploy/helm/pvlogger',
    name='pvlogger',
    namespace=NAMESPACE,
    values=['deploy/dev/values-dev.yaml'],
    set=['global.timezone=' + timezone],
))

# Not in the chart: the dev database and the history backfill.
k8s_yaml(dev_manifest("deploy/dev/postgres.yaml"))
k8s_yaml(dev_manifest('deploy/dev/backfill-job.yaml'))

# --- Resources --------------------------------------------------------------

k8s_resource('pvlogger-dev-postgres', labels=['dev'], port_forwards=['15432:5432'],
             objects=['pvlogger-dev-postgres-init:configmap'])
k8s_resource('pvlogger-modbus-mock-server', labels=['dev'])

k8s_resource('pvlogger-modbus-data-accessor', labels=['services'], resource_deps=['pvlogger-dev-postgres'],
             port_forwards=['8083:8083'])
k8s_resource('pvlogger-masterdata-server', labels=['services'], resource_deps=['pvlogger-dev-postgres'],
             port_forwards=['8081:8081'])
k8s_resource('pvlogger-frontend', labels=['services'], resource_deps=['pvlogger-modbus-data-accessor'],
             links=[link('http://localhost/', 'dashboard')])

k8s_resource('pvlogger-backfill', labels=['dev'], resource_deps=['pvlogger-modbus-data-accessor'])

# The full and device collectors read the register catalog from the
# masterdata server. The import is idempotent (unique register address).
local_resource(
    'masterdata-import',
    cmd='for i in 1 2 3 4 5 6 7 8 9 10; do ' +
        'curl -sSf -X PUT http://localhost:8081/import-master-data && exit 0; sleep 3; done; exit 1',
    resource_deps=['pvlogger-masterdata-server'],
    labels=['dev'],
)

# The collectors run on their chart schedules (the fast one every minute).
# The buttons start a run now, like `kubectl create job --from=cronjob/...`.
COLLECTOR_CRONJOBS = [
    'pv-fast-data-collector-cronjob',
    'pv-full-data-collector-cronjob',
    'pv-device-static-data-collector-cronjob',
    'pv-device-identifiers-cronjob',
    'pv-device-list-cronjob',
]

for cronjob in COLLECTOR_CRONJOBS:
    k8s_resource(cronjob, labels=['collectors'], resource_deps=['pvlogger-modbus-mock-server'])
    local_resource(
        'run-' + cronjob.replace('-cronjob', ''),
        cmd='kubectl --context %s -n %s create job --from=cronjob/%s %s-$(date +%%s)' % (
            expected_context, NAMESPACE, cronjob, cronjob.replace('-cronjob', '')),
        trigger_mode=TRIGGER_MODE_MANUAL,
        # A click before the import has run waits for it: the full and device
        # collectors fail without the register catalog.
        resource_deps=['masterdata-import'],
        auto_init=False,
        labels=['collectors'],
    )
