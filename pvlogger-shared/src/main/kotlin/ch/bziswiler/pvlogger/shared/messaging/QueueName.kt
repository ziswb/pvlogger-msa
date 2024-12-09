package ch.bziswiler.pvlogger.shared.messaging

enum class QueueName(val value: String) {
    JOB_SCHEDULER("job.scheduler"),
    PERSIST_MODBUS("persist.modbus"),
    KPI_EVENTS("kpi.events")
}