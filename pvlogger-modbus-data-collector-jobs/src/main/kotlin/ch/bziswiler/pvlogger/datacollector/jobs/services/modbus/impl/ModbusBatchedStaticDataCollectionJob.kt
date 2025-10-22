package ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.AppConfig
import ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.jobs.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.STATIC_DATA_JOB
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.staticDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.staticDataCollectionJobSucceededMessage
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.CONFIG
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.STATIC
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Metrics.counter
import io.micrometer.core.instrument.Timer
import java.time.Duration

class ModbusBatchedStaticDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: StoreModbusValuesMessageSender,
    jobSchedulerMessageSender: JobSchedulerMessageSender
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender, jobSchedulerMessageSender) {

    override val jobName = STATIC_DATA_JOB

    private val counterId = "modbus-batched-static-data-collection-job-counter"
    private val timerId = "modbus-batched-static-data-collection-job-timer"

    override val categories = listOf(STATIC, CONFIG)
    override val metricCounter: Counter = counter(counterId)
    override val metricTimer: Timer =
        Timer
            .builder(timerId)
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(1500))
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofMillis(10000))
            .register(Metrics.globalRegistry)

    override fun jobSucceededMessageObject(jobId: JobId) = staticDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = staticDataCollectionJobFailedMessage(jobId)
}
