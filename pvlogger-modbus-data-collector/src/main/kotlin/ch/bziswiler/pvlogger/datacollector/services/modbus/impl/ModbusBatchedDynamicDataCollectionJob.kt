package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.PersistenceMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.ALARM
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.DYNAMIC
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.dynamicDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.dynamicDataCollectionJobSucceededMessage
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.JobName.DYNAMIC_DATA_JOB
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Metrics.counter
import io.micrometer.core.instrument.Timer
import java.time.Duration

class ModbusBatchedDynamicDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    persistenceMessageSender: PersistenceMessageSender,
    jobSchedulerMessageSender: JobSchedulerMessageSender
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService, persistenceMessageSender, jobSchedulerMessageSender) {

    private val counterId = "modbus-batched-dynamic-data-collection-job-counter"
    private val timerId = "modbus-batched-dynamic-data-collection-job-timer"

    override val jobName = DYNAMIC_DATA_JOB
    override val categories = listOf(DYNAMIC, ALARM)
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

    override fun jobFinishedMessageObject(jobId: JobId) = dynamicDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = dynamicDataCollectionJobFailedMessage(jobId)
}
