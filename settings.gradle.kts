rootProject.name = "pvlogger-msa"
include(
    "pvlogger-database",
    "pvlogger-heartbeat",
    "pvlogger-k8s",
    "pvlogger-kpi-aggregator",
    "pvlogger-masterdata-server",
    "pvlogger-message-queue",
    "pvlogger-modbus-data-accessor",
    "pvlogger-modbus-data-collector",
    "pvlogger-modbus-mock-server",
    "pvlogger-shared"
)
