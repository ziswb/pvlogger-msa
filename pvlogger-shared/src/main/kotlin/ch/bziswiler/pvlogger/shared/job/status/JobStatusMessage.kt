package ch.bziswiler.pvlogger.shared.job.status

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import java.io.Serializable

data class JobStatusMessage(val messageTag: MessageTag, val jobId: JobId, val status: JobStatus) : Serializable
