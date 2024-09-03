package ch.bziswiler.pvlogger.messaging.job.status

import ch.bziswiler.pvlogger.messaging.job.status.JobStatus.FAILED
import ch.bziswiler.pvlogger.messaging.job.status.JobStatus.FINISHED
import ch.bziswiler.pvlogger.messaging.meta.JobFinishedMessageTag
import ch.bziswiler.pvlogger.messaging.meta.JobId
import ch.bziswiler.pvlogger.messaging.meta.JobName
import ch.bziswiler.pvlogger.messaging.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.meta.JobName.STATIC_DATA_JOB

object JobStatusMessageFactory {

    fun dynamicDataCollectionJobFinishedMessage(jobId: JobId) = jobFinishedMessage(DYNAMIC_DATA_JOB, jobId)
    fun dynamicDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(DYNAMIC_DATA_JOB, jobId)
    fun staticDataCollectionJobFinishedMessage(jobId: JobId) = jobFinishedMessage(STATIC_DATA_JOB, jobId)
    fun staticDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(STATIC_DATA_JOB, jobId)
    fun deviceIdsDataCollectionJobFinishedMessage(jobId: JobId) = jobFinishedMessage(DEVICE_IDS_JOB, jobId)
    fun deviceListDataCollectionJobFinishedMessage(jobId: JobId) = jobFinishedMessage(DEVICE_LIST_JOB, jobId)

    private fun jobFinishedMessage(jobName: JobName, jobId: JobId) = JobStatusMessage(JobFinishedMessageTag(jobName), jobId, FINISHED)
    private fun jobFailedMessage(jobName: JobName, jobId: JobId) = JobStatusMessage(JobFinishedMessageTag(jobName), jobId, FAILED)
}