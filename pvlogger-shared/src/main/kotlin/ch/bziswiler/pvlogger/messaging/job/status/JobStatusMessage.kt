package ch.bziswiler.pvlogger.messaging.job.status

import ch.bziswiler.pvlogger.messaging.job.tag.JobTag
import java.io.Serializable

data class JobStatusMessage(val jobTag: JobTag, val jobId: String, val status: JobStatus) : Serializable
