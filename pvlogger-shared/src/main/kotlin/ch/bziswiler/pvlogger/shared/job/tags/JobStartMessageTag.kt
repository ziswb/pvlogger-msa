package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class JobStartMessageTag(private val jobName: JobName) : MessageTag {
    override val key = "job"
    override val value = jobName.value
    override fun toSelector() = "$key = '${value}'"
}