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
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_create_namespace/files/postgresql-namespace.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/postgresql_install/files/postgresql-tilt-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-namespace.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-tilt-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/activemq/files/activemq-service.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_common_install/files/pvlogger-namespace.yaml')
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
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_heartbeat_install/files/pvlogger-heartbeat-deployment.yaml')
k8s_yaml('pvlogger-k8s/pvlogger-setup/roles/pvlogger_heartbeat_install/files/pvlogger-heartbeat-service.yaml')
