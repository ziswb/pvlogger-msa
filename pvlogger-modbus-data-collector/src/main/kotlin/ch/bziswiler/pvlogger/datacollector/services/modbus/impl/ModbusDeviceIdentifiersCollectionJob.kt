package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.request.ReadDeviceIdentifiersRequest
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.impl.PersistenceMessageSender
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.shared.messaging.meta.StoreModbusRegisterValuesMessageTag
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime.now

@OptIn(ExperimentalUnsignedTypes::class)
open class ModbusDeviceIdentifiersCollectionJob(
    config: AppConfig,
    private val channel: ModbusTcpChannel,
    protected val idProvider: IdProvider,
    private val persistenceMessageSender: PersistenceMessageSender
) : ModbusDataCollectionJob {

    private val logger = KotlinLogging.logger {}

    private val inverterDeviceIds = config.modbusTcpConfig.inverterDeviceIds
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

    private fun executeForEachDeviceId(jobId: String) = inverterDeviceIds.forEach { deviceId ->
        executeForSingleDeviceId(jobId, deviceId)
    }

    private fun executeForSingleDeviceId(
        jobId: String,
        deviceId: UByte,
    ) = runCatching {
        readDeviceIdentifiers(jobId, deviceId)
    }.onSuccess {
        persist(it)
    }.onFailure {
        logger.error(it) { "Fetching device identifiers for device '$deviceId' failed" }
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
