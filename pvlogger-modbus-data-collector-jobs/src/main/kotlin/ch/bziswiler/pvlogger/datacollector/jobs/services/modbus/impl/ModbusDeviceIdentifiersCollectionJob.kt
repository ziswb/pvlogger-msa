package ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.Settings
import ch.bziswiler.pvlogger.datacollector.jobs.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.jobs.services.messaging.StoreModbusValuesMessageSender
import ch.bziswiler.pvlogger.datacollector.jobs.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.modbus.api.meta.JobId
import ch.bziswiler.pvlogger.modbus.api.meta.JobName
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.modbus.tcp.request.ReadDeviceIdentifiersRequest
import ch.bziswiler.pvlogger.modbus.tcp.response.ReadDeviceIdentifiersResponse
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceIdsDataCollectionJobFailedMessage
import ch.bziswiler.pvlogger.shared.job.status.JobStatusMessageFactory.deviceIdsDataCollectionJobSucceededMessage
import ch.bziswiler.pvlogger.shared.job.tags.StoreModbusRegisterValuesMessageTag
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime.now

@OptIn(ExperimentalUnsignedTypes::class)
open class ModbusDeviceIdentifiersCollectionJob(
    settings: Settings,
    private val channel: ModbusTcpChannel,
    protected val idProvider: IdProvider,
    private val persistenceMessageSender: StoreModbusValuesMessageSender,
) : ModbusDataCollectionJob {
    override val jobName = JobName.DEVICE_IDS_JOB

    private val logger = KotlinLogging.logger {}

    private val inverterDeviceIds = settings.modbusTcpConfig.inverterDeviceIds
    private val counter = Metrics.counter("modbus-device-identifiers-collection-job-execution")
    private val timer =
        Timer
            .builder("modbus_device_identifiers_collection_job_timer")
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(1500))
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofMillis(10000))
            .register(Metrics.globalRegistry)

    override fun execute() {
        counter.increment()
        timer.recordCallable { executeForEachDeviceId(idProvider.nextJobId) }
    }

    override fun jobSucceededMessageObject(jobId: JobId) = deviceIdsDataCollectionJobSucceededMessage(jobId)

    override fun jobFailedMessageObject(jobId: JobId) = deviceIdsDataCollectionJobFailedMessage(jobId)

    private fun executeForEachDeviceId(jobId: String) =
        inverterDeviceIds.forEach { deviceId ->
            executeForSingleDeviceId(jobId, deviceId)
        }

    private fun executeForSingleDeviceId(
        jobId: String,
        deviceId: UByte,
    ) = runCatching {
        logger.debug { "ModbusDeviceIdentifiersCollectionJob.executeForSingleDeviceId().runCatching()" }
        readDeviceIdentifiers(jobId, deviceId)
    }.onSuccess {
        logger.debug { "ModbusDeviceIdentifiersCollectionJob.executeForSingleDeviceId().onSuccess()" }
        persist(it)
    }.onFailure {
        logger.error(it) { "Fetching device identifiers for device '$deviceId' failed" }
        throw it
    }

    private fun persist(response: ReadDeviceIdentifiersResponse) {
        logger.debug { "Storing device identifiers $response" }
        persistenceMessageSender.tagAndSendMessage(response, StoreModbusRegisterValuesMessageTag())
    }

    private fun readDeviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ): ReadDeviceIdentifiersResponse =
        runBlocking {
            logger.debug { "ModbusDeviceIdentifiersCollectionJob.readAndStoreDeviceIdentifiers()" }
            return@runBlocking channel.request(deviceIdentifiers(jobId, deviceId)) as ReadDeviceIdentifiersResponse
        }

    protected open fun deviceIdentifiers(
        jobId: String,
        deviceId: UByte,
    ) = ReadDeviceIdentifiersRequest(jobId, now(), idProvider.nextRequestId, deviceId)
}
