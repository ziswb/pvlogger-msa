package ch.bziswiler.pvlogger.datacollector.services.modbus.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.ModbusTcpChannel
import ch.bziswiler.pvlogger.datacollector.services.IdProvider
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.datacollector.services.modbus.MasterDataService
import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusBatchedDataCollectionJob
import ch.bziswiler.pvlogger.masterdata.modbus.Category.CONFIG
import ch.bziswiler.pvlogger.masterdata.modbus.Category.STATIC
import ch.bziswiler.pvlogger.messaging.job.status.JobStatusMessageFactory.staticDataCollectionJobFinishedMessage
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Metrics.counter
import io.micrometer.core.instrument.Timer
import mu.KotlinLogging
import java.time.Duration

class ModbusBatchedStaticDataCollectionJob(
    config: AppConfig,
    channel: ModbusTcpChannel,
    idProvider: IdProvider,
    masterDataService: MasterDataService,
    private val mqService: MqService
) : ModbusBatchedDataCollectionJob(config, channel, idProvider, masterDataService) {
    private val logger = KotlinLogging.logger {}
    override val categories = listOf(STATIC, CONFIG)
    override val metricCounter: Counter = counter("modbus-inverter-batched-static-data-collection-job-execution")
    override val metricTimer: Timer =
        Timer
            .builder("modbus_inverter_batched_static_data_collection_job_timer")
            .publishPercentiles(0.5, 0.95)
            .publishPercentileHistogram()
            .serviceLevelObjectives(Duration.ofMillis(1500))
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofMillis(10000))
            .register(Metrics.globalRegistry)

    override fun storeResponse(response: ModbusResponse) {
        logger.debug("ModbusInverterBatchedStaticDataCollectionJob.storeResponse()")
        when (response) {
            is ModbusRegisterValueResponse -> mqService.sendMessage(response)
            else -> throw UnsupportedOperationException("'storeResponse()' does not support '${response::class.java.canonicalName}'")
        }
    }

    override fun jobFinished(jobId: String) {
        mqService.sendJobStatusMessage(staticDataCollectionJobFinishedMessage(jobId))
    }
}
