plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "pvlogger-msa"
include(
    "pvlogger-frontend",
    "pvlogger-masterdata-server",
    "pvlogger-modbus-data-accessor",
    "pvlogger-modbus-mock-server",
    "pvlogger-sunrise-sunset",
    "pvlogger-shared",
)
