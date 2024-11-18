package ch.bziswiler.pvlogger.shared.job.status

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_IDS_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DEVICE_LIST_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.meta.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatus.FAILURE
import ch.bziswiler.pvlogger.shared.job.status.JobStatus.SUCCESS
import ch.bziswiler.pvlogger.shared.job.tags.JobFinishedMessageTag

object JobStatusMessageFactory {

    fun dynamicDataCollectionJobSucceededMessage(jobId: JobId) = jobSucceededMessage(DYNAMIC_DATA_JOB, jobId)
    fun dynamicDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(DYNAMIC_DATA_JOB, jobId)
    fun staticDataCollectionJobSucceededMessage(jobId: JobId) = jobSucceededMessage(STATIC_DATA_JOB, jobId)
    fun staticDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(STATIC_DATA_JOB, jobId)
    fun deviceIdsDataCollectionJobSucceededMessage(jobId: JobId) = jobSucceededMessage(DEVICE_IDS_JOB, jobId)
    fun deviceIdsDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(DEVICE_IDS_JOB, jobId)
    fun deviceListDataCollectionJobSucceededMessage(jobId: JobId) = jobSucceededMessage(DEVICE_LIST_JOB, jobId)
    fun deviceListDataCollectionJobFailedMessage(jobId: JobId) = jobFailedMessage(DEVICE_LIST_JOB, jobId)

    private fun jobSucceededMessage(jobName: JobName, jobId: JobId) = JobStatusMessage(JobFinishedMessageTag(jobName), jobId, SUCCESS)
    private fun jobFailedMessage(jobName: JobName, jobId: JobId) = JobStatusMessage(JobFinishedMessageTag(jobName), jobId, FAILURE)
}