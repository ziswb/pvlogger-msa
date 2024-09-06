package ch.bziswiler.pvlogger.shared.messaging.job.status

import ch.bziswiler.pvlogger.shared.messaging.meta.JobId
import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import java.io.Serializable

data class JobStatusMessage(val messageTag: MessageTag, val jobId: JobId, val status: JobStatus) : Serializable
