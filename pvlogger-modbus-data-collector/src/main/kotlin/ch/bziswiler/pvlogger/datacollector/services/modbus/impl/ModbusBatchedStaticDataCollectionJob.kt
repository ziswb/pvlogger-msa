package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.datacollector.services.modbus.request.ModbusTcpChannel
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.Category.CONFIG
import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.Category.STATIC
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.staticDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.staticDataCollectionJobSucceededMessage

class ModbusBatchedStaticDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: StoreModbusValuesMessageSender,
    jobSchedulerMessageSender: JobSchedulerMessageSender
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender, jobSchedulerMessageSender) {

    override val jobName = STATIC_DATA_JOB
    override val categories = listOf(STATIC, CONFIG)

    override fun jobSucceededMessageObject(jobId: JobId) = staticDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = staticDataCollectionJobFailedMessage(jobId)
}
