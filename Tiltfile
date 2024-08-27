version_settings(constraint='>=0.22.2')
k8s_context('docker-desktop')

pg_pw_yml = """
apiVersion: v1
data:
  POSTGRES_PASSWORD: "<redacted>"
kind: Secret
metadata:
  name: postgresql-secret
  namespace: postgresql
type: kubernetes.io/generic
"""
k8s_yaml([
    blob(pg_pw_yml)
])
pvol_yml = """
apiVersion: v1
kind: PersistentVolume
metadata:
  name: nfs-storage
  labels:
    type: local
spec:
  capacity:
    storage: 1Ti
  accessModes:
    - ReadWriteMany
    - ReadWriteOnce
    - ReadOnlyMany
  storageClassName: nfs-client
  persistentVolumeReclaimPolicy: Retain
  volumeMode: Filesystem
  hostPath:
    path: "/Users/bziswiler/pv-data"
"""
k8s_yaml([
    blob(pvol_yml)
])

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_create_namespace/files/postgresql-namespace.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-pvc-backups.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-pvc-data.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-pvc-logs.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-pvc-os.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-service.yaml')

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-namespace.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-claim0-persistentvolumeclaim.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-claim1-persistentvolumeclaim.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-service.yaml')

pv_pw_yml = """
apiVersion: v1
data:
  POSTGRES_PASSWORD: "<redacted>"
kind: Secret
metadata:
  name: postgresql-secret
  namespace: pvlogger
type: kubernetes.io/generic
"""
k8s_yaml([
    blob(pv_pw_yml)
])

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_common_install/files/pvlogger-namespace.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_heartbeat_install/files/pvlogger-heartbeat-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_heartbeat_install/files/pvlogger-heartbeat-service.yaml')

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_masterdata_server_install/files/pvlogger-masterdata-server-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_masterdata_server_install/files/pvlogger-masterdata-server-service.yaml')

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_modbus_data_accessor_install/files/pvlogger-modbus-data-accessor-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_modbus_data_accessor_install/files/pvlogger-modbus-data-accessor-service.yaml')

k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_modbus_data_collector_install/files/pvlogger-modbus-data-collector-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_modbus_data_collector_install/files/pvlogger-modbus-data-collector-service.yaml')
