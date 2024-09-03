package ch.bziswiler.pvlogger.messaging.meta

data class JobStartMessageTag(private val jobName: JobName) : MessageTag {
    override val key = "job"
    override val value = jobName.value
    override fun toSelector() = "$key = '${value}'"
}