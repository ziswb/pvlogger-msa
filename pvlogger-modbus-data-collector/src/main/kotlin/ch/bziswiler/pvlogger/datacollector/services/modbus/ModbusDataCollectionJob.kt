package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.messaging.meta.JobId
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName

interface ModbusDataCollectionJob {
    val jobName: JobName
    fun jobFinishedMessageObject(jobId: JobId): JobStatusMessage
    fun jobFailedMessageObject(jobId: JobId): JobStatusMessage
    fun execute()
}