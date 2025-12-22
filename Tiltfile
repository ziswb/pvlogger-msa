version_settings(constraint='>=0.22.2')
k8s_context('docker-desktop')
allow_k8s_contexts('docker-desktop')

pg_pw_yml = """
apiVersion: v1
data:
  POSTGRES_PASSWORD: """ + str(local('./scripts/get_postgres_pw_base64_encoded.sh')) + """
kind: Secret
metadata:
  name: postgresql-secret
  namespace: postgresql
type: kubernetes.io/generic
"""
k8s_yaml([
    blob(pg_pw_yml)
])

fs_pw_yml = """
apiVersion: v1
data:
  FUSIONSOLAR_PASSWORD: """ + str(local('./scripts/get_fusion_solar_pw_base64_encoded.sh')) + """
kind: Secret
metadata:
  name: fusionsolar-secret
  namespace: pvlogger
type: kubernetes.io/generic
"""
k8s_yaml([
    blob(fs_pw_yml)
])

local_resource('install_traefik', cmd='./scripts/install_traefik.sh')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_common_install/files/pvlogger-db-configmap.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_common_install/files/pvlogger-namespace.yaml')

k8s_yaml(local('./scripts/replace_path_by_home.sh ./tilt-templates/pg-backup-data-pv-template.yaml'))
k8s_yaml(local('./scripts/replace_path_by_home.sh ./tilt-templates/pg-data-data-pv-template.yaml'))
k8s_yaml(local('./scripts/replace_path_by_home.sh ./tilt-templates/pg-logs-data-pv-template.yaml'))
k8s_yaml(local('./scripts/replace_path_by_home.sh ./tilt-templates/pg-os-data-pv-template.yaml'))
k8s_yaml(local('./scripts/replace_path_by_home.sh ./tilt-templates/kpi-export-data-pv-template.yaml'))
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/postgresql_create_namespace/files/postgresql-namespace.yaml')
k8s_yaml('./tilt-templates/postgresql-pvc-backups.yaml')
k8s_yaml('./tilt-templates/postgresql-pvc-data.yaml')
k8s_yaml('./tilt-templates/postgresql-pvc-logs.yaml')
k8s_yaml('./tilt-templates/postgresql-pvc-os.yaml')
k8s_yaml('./tilt-templates/postgresql-deployment.yaml')
k8s_yaml('./tilt-templates/postgresql-service.yaml')

pv_pw_yml = """
apiVersion: v1
data:
  POSTGRES_PASSWORD: """ + str(local('./scripts/get_postgres_pw_base64_encoded.sh')) + """
kind: Secret
metadata:
  name: postgresql-secret
  namespace: pvlogger
type: kubernetes.io/generic
"""
k8s_yaml([
    blob(pv_pw_yml)
])

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_masterdata_server_install/files/pvlogger-masterdata-server-deployment.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_masterdata_server_install/files/pvlogger-masterdata-server-service.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_data_accessor_install/files/pvlogger-modbus-data-accessor-deployment.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_data_accessor_install/files/pvlogger-modbus-data-accessor-service.yaml')

cm_yml = """
apiVersion: v1
kind: ConfigMap
metadata:
  name: lwip-config
  namespace: pvlogger
data:
  MODBUS_HOSTNAME: "pvlogger-modbus-mock-server-service.pvlogger.svc.cluster.local"
  MODBUS_PORT: "8502"
  MODBUS_TIMEOUT: "5"
"""
k8s_yaml([
    blob(cm_yml)
])

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_data_collector_go_install/files/pvlogger-modbus-data-collector-configmap.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_data_collector_go_install/files/pvlogger-modbus-data-collector-deployment.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_mock_server_install/files/pvlogger-modbus-mock-server-deployment.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_modbus_mock_server_install/files/pvlogger-modbus-mock-server-service.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_kpi_aggregator_install/files/pvlogger-kpi-aggregator-deployment.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_kpi_exporter_install/files/pvlogger-kpi-exporter-cm.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_kpi_exporter_install/files/pvlogger-kpi-exporter-cronjob.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_kpi_exporter_install/files/pvlogger-kpi-exporter-pvc.yaml')

tf_mw_yml = """
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: strip-pv
  namespace: pvlogger
spec:
  stripPrefix:
    prefixes:
      - /pv
"""
k8s_yaml([
    blob(tf_mw_yml)
])

tf_dt_yml = """
apiVersion: traefik.io/v1alpha1
kind: IngressRoute
metadata:
  name: pvlogger-data
  namespace: pvlogger
spec:
  entryPoints:
    - web
  routes:
    - kind: Rule
      match: Host(`localhost`) && PathPrefix(`/v1`)
      priority: 10
      services:
        - kind: Service
          name: pvlogger-modbus-data-accessor-service
          port: pvlogger-data-accessor
          nativeLB: true
    - kind: Rule
      match: Host(`localhost`) && PathPrefix(`/livedata`)
      priority: 100
      services:
        - kind: Service
          name: pvlogger-frontend-service
          port: pvlogger-frontend
          nativeLB: true
    - kind: Rule
      match: Host(`localhost`) && PathPrefix(`/pv`)
      priority: 1000
      middlewares:
        - name: "strip-pv"
          namespace: pvlogger
      services:
        - kind: Service
          name: pvlogger-frontend-service
          port: pvlogger-frontend
          nativeLB: true
"""
k8s_yaml([
    blob(tf_dt_yml)
])

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_frontend_install/files/pvlogger-frontend-deployment.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_frontend_install/files/pvlogger-frontend-service.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_sunrise_sunset_install/files/pvlogger-crepuscular-deployment.yaml')

k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cm.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cm-1.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cm-2.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cm-3.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cm-4.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cronjob-1.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cronjob-2.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cronjob-3.yaml')
k8s_yaml('pvlogger-infrastructure/workload-setup/roles/pvlogger_fusionsolar_install/files/pvlogger-fusionsolar-cronjob-4.yaml')
