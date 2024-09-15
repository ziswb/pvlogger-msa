package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class JobFinishedMessageTag(private val jobName: JobName) : MessageTag {
    override val key = "job"
    override val value = "${jobName.value}.finished"
    override fun toSelector() = "$key = '${value}'"
}