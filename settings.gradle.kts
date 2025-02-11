plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "pvlogger-msa"
include(
    "pvlogger-crepuscular-service",
    "pvlogger-database",
    "pvlogger-frontend",
    "pvlogger-fusionsolar",
    "pvlogger-heartbeat",
    "pvlogger-infrastructure",
    "pvlogger-kpi-aggregator",
    "pvlogger-kpi-exporter",
    "pvlogger-masterdata-server",
    "pvlogger-message-queue",
    "pvlogger-modbus-data-accessor",
    "pvlogger-modbus-data-collector",
    "pvlogger-modbus-mock-server",
    "pvlogger-shared",
)
