package ch.bziswiler.pvlogger.datacollector.jobs.services.modbus

import ch.bziswiler.pvlogger.shared.job.PvJob
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessage

interface ModbusDataCollectionJob: PvJob {
    val jobName: JobName
    fun jobSucceededMessageObject(jobId: JobId): JobStatusMessage
    fun jobFailedMessageObject(jobId: JobId): JobStatusMessage
}