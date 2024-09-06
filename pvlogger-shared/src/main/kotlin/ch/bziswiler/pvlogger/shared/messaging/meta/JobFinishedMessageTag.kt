package ch.bziswiler.pvlogger.shared.messaging.meta

data class JobFinishedMessageTag(private val jobName: JobName) : MessageTag {
    override val key = "job"
    override val value = "${jobName.value}.finished"
    override fun toSelector() = "$key = '${value}'"
}