package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ModbusTcpChannel
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.Category.ALARM
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.Category.DYNAMIC
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.dynamicDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.dynamicDataCollectionJobSucceededMessage

class ModbusBatchedDynamicDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: StoreModbusValuesMessageSender,
    jobSchedulerMessageSender: JobSchedulerMessageSender
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender, jobSchedulerMessageSender) {

    override val jobName = DYNAMIC_DATA_JOB
    override val categories = listOf(DYNAMIC, ALARM)

    override fun jobSucceededMessageObject(jobId: JobId) = dynamicDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = dynamicDataCollectionJobFailedMessage(jobId)
}
