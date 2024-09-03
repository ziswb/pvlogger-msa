package ch.bziswiler.pvlogger.messaging.job.status

import ch.bziswiler.pvlogger.messaging.meta.JobId
import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import java.io.Serializable

data class JobStatusMessage(val messageTag: MessageTag, val jobId: JobId, val status: JobStatus) : Serializable
