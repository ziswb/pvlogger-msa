package ch.bziswiler.pvlogger.messaging.job.tag

import ch.bziswiler.pvlogger.messaging.job.naming.JobName
import java.io.Serializable

data class JobTag(val value: JobName, val key: String = "job") : Serializable {
    fun toSelector() = "$key = '${value.value}'"
}