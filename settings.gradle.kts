plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "pvlogger-msa"
include(
    "pvlogger-database",
    "pvlogger-frontend",
    "pvlogger-fusionsolar",
    "pvlogger-heartbeat",
    "pvlogger-infrastructure",
    "pvlogger-kpi-aggregator",
    "pvlogger-kpi-exporter",
    "pvlogger-masterdata-server",
    "pvlogger-message-queue",
    "pvlogger-modbus-tcp-api",
    "pvlogger-modbus-tcp-impl",
    "pvlogger-modbus-data-accessor",
    "pvlogger-modbus-data-collector",
    "pvlogger-modbus-mock-server",
    "pvlogger-sunrise-sunset",
    "pvlogger-shared",
)
