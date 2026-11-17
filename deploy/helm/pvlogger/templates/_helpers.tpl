{{/*
Expand the name of the chart.
*/}}
{{- define "pvlogger.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "pvlogger.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "pvlogger.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "pvlogger.labels" -}}
helm.sh/chart: {{ include "pvlogger.chart" . }}
{{ include "pvlogger.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "pvlogger.selectorLabels" -}}
app.kubernetes.io/name: {{ include "pvlogger.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the image name
*/}}
{{- define "pvlogger.image" -}}
{{- $registry := .global.imageRegistry -}}
{{- $repository := .image.repository -}}
{{- /* No fallback to "latest": build.sh pushes semver tags only */ -}}
{{- $tag := required (printf "image.tag is not set for %s" $repository) .image.tag -}}
{{- if $registry -}}
{{- printf "%s/%s:%s" $registry $repository $tag -}}
{{- else -}}
{{- printf "%s:%s" $repository $tag -}}
{{- end -}}
{{- end }}

{{/*
In-cluster host names of the services, derived from the release namespace
(the Service names are fixed in data-accessor-service.yaml and
masterdata-server-service.yaml).
*/}}
{{- define "pvlogger.dataAccessorHost" -}}
pvlogger-modbus-data-accessor-service.{{ .Release.Namespace }}.svc.cluster.local
{{- end }}

{{- define "pvlogger.masterdataHost" -}}
pvlogger-masterdata-server-service.{{ .Release.Namespace }}.svc.cluster.local
{{- end }}

{{/*
Common timezone environment variable
*/}}
{{- define "pvlogger.timezoneEnv" -}}
- name: TZ
  value: {{ .Values.global.timezone | quote }}
{{- end }}

{{/*
Pod annotations that roll a Deployment when its env-sourced ConfigMaps or
Secrets change (envFrom/secretKeyRef are read only at container start).
pvlogger.checksum hashes a rendered template without its helm.sh/chart label,
which changes with every chart version: otherwise every release would restart
these Deployments (seen with 2.0.1).
*/}}
{{- define "pvlogger.checksum" -}}
{{- regexReplaceAll "(?m)^\\s*helm\\.sh/chart:.*$" (include (print .root.Template.BasePath .file) .root) "" | sha256sum }}
{{- end }}

{{- define "pvlogger.configChecksums" -}}
checksum/config: {{ include "pvlogger.checksum" (dict "root" . "file" "/configmaps.yaml") }}
checksum/secrets: {{ include "pvlogger.checksum" (dict "root" . "file" "/secrets.yaml") }}
{{- end }}

{{/*
Container hardening for the JVM services. The images
already run as 1000:1000 with no capabilities; this pins that and forbids
privilege escalation.
*/}}
{{- define "pvlogger.jvmSecurityContext" -}}
securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  runAsNonRoot: true
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: ["ALL"]
  seccompProfile:
    type: RuntimeDefault
{{- end }}

{{/*
Container hardening for the Go jobs. Their images are
distroless :nonroot; the network clients and the KPI exporter write nothing, so
the root filesystem is read-only. Pass the uid: (dict "uid" .Values.global.runAsUid).
*/}}
{{- define "pvlogger.goSecurityContext" -}}
securityContext:
  runAsUser: {{ .uid }}
  runAsGroup: {{ .uid }}
  runAsNonRoot: true
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: ["ALL"]
  seccompProfile:
    type: RuntimeDefault
{{- end }}

{{/*
A writable /tmp for a container with a read-only root filesystem: the JVM
writes its perf data and Netty its native library there, db-backup its
.pgpass. Volume and mount; the volume goes under the pod's volumes.
*/}}
{{- define "pvlogger.tmpVolume" -}}
- name: tmp
  emptyDir:
    sizeLimit: 64Mi
{{- end }}

{{- define "pvlogger.tmpVolumeMount" -}}
- name: tmp
  mountPath: /tmp
{{- end }}

{{/*
Labels the Tailscale operator puts on the proxy pod of one Ingress, for the
NetworkPolicies: only that ingress's proxy, not every proxy in the operator's
namespace. Args: root (the chart context), ingress (the Ingress name).
*/}}
{{- define "pvlogger.tailscaleProxyLabels" -}}
tailscale.com/managed: "true"
tailscale.com/parent-resource-type: ingress
tailscale.com/parent-resource: {{ .ingress }}
tailscale.com/parent-resource-ns: {{ .root.Release.Namespace }}
{{- end }}
