package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessage

interface ModbusDataCollectionJob {
    val jobName: JobName
    fun jobFinishedMessageObject(jobId: JobId): JobStatusMessage
    fun jobFailedMessageObject(jobId: JobId): JobStatusMessage
    fun execute()
}