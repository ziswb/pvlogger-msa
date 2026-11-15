plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "pvlogger-msa"
include(
    "pvlogger-frontend",
    "pvlogger-masterdata-server",
    "pvlogger-modbus-data-accessor",
    "pvlogger-shared-database",
    "pvlogger-shared-ktor",
    "pvlogger-shared-model",
    "pvlogger-shared-rest",
)
