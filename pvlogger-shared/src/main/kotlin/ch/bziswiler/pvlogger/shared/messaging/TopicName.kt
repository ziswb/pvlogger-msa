package ch.bziswiler.pvlogger.shared.messaging

enum class TopicName(override val value: String) : Name {
    JOB_SCHEDULER("job.scheduler"),
    KPI_EVENTS("kpi.events")
}