package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.JobSchedulerMessageSender
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.PersistenceMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.CONFIG
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category.STATIC
import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessageFactory.staticDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessageFactory.staticDataCollectionJobFinishedMessage
import ch.bziswiler.pvlogger.shared.messaging.meta.JobId
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName.STATIC_DATA_JOB
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
    persistenceMessageSender: PersistenceMessageSender,
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

    override fun jobFinishedMessageObject(jobId: JobId) = staticDataCollectionJobFinishedMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = staticDataCollectionJobFailedMessage(jobId)
}
