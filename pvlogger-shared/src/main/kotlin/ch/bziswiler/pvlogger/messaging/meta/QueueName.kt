package ch.bziswiler.pvlogger.messaging.meta

enum class QueueName(val value: String) {
    JOB_SCHEDULER("job.scheduler"),
    PERSIST_MODBUS("persist.modbus")
}