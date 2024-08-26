package ch.bziswiler.pvlogger.messaging.job.status

import ch.bziswiler.pvlogger.messaging.job.naming.JobName
import ch.bziswiler.pvlogger.messaging.job.naming.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.messaging.job.naming.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.messaging.job.naming.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.job.naming.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.job.status.JobStatus.FINISHED
import ch.bziswiler.pvlogger.messaging.job.tag.JobTag

object JobStatusMessageFactory {

    fun dynamicDataCollectionJobFinishedMessage(jobId: String) = jobFinishedMessage(DYNAMIC_DATA_JOB, jobId)
    fun staticDataCollectionJobFinishedMessage(jobId: String) = jobFinishedMessage(STATIC_DATA_JOB, jobId)
    fun deviceIdsDataCollectionJobFinishedMessage(jobId: String) = jobFinishedMessage(DEVICE_IDS_JOB, jobId)
    fun deviceListDataCollectionJobFinishedMessage(jobId: String) = jobFinishedMessage(DEVICE_LIST_JOB, jobId)

    private fun jobFinishedMessage(jobName: JobName, jobId: String) = JobStatusMessage(JobTag(jobName), jobId, FINISHED)
}