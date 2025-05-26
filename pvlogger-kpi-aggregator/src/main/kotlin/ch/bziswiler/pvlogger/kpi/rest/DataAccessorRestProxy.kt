package ch.bziswiler.pvlogger.kpi.rest

import ch.bziswiler.pvlogger.kpi.configuration.ModbusDataAccessConfig
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForInverterActivePower
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForInverterEnergyYield
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForMeterExportedEnergy
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForMeterImportedEnergy
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForSmartMeterActivePower
import ch.bziswiler.pvlogger.kpi.rest.data.InverterActivePower
import ch.bziswiler.pvlogger.kpi.rest.data.InverterEnergyToday
import ch.bziswiler.pvlogger.kpi.rest.data.MeterActivePower
import ch.bziswiler.pvlogger.kpi.rest.data.MeterEnergyToday
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.Measurement
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode.Companion.OK
import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class DataAccessorRestProxy(private val config: ModbusDataAccessConfig) {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun inverterActivePower(jobId: JobId) = InverterActivePower(retrieveLatestRegisterValuesAggregated(masterDataForInverterActivePower(), config.inverterDeviceIds, jobId), masterDataForInverterActivePower().unit)

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun meterActivePower(jobId: JobId) = MeterActivePower(retrieveLatestRegisterValuesAggregated(masterDataForSmartMeterActivePower(), config.meterDeviceIds, jobId), masterDataForSmartMeterActivePower().unit)

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun inverterEnergyToday(jobId: JobId) = InverterEnergyToday(retrieveLatestRegisterValuesAggregated(masterDataForInverterEnergyYield(), config.inverterDeviceIds, jobId), masterDataForInverterEnergyYield().unit)

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun meterEnergyToday() = MeterEnergyToday(computeImportedEnergyToday(config.meterDeviceIds), masterDataForMeterImportedEnergy().unit, computeExportedEnergyToday(config.meterDeviceIds), masterDataForMeterExportedEnergy().unit)

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun retrieveLatestRegisterValuesAggregated(masterData: ModbusRegisterDefinition, deviceIds: UByteArray, jobId: JobId) = deviceIds.fold(Measurement(now(), 0.0)) { sum, deviceId ->
        val measurement = retrieveLatestRegisterValue(masterData, deviceId, jobId)
        Measurement(measurement.timestamp, sum.value + measurement.value)
    }

    private suspend fun retrieveLatestRegisterValue(masterData: ModbusRegisterDefinition, deviceId: UByte, jobId: JobId): Measurement<Double> = client.get {
        logger.info { "Getting actual power for $masterData" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(
                DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH,
                masterData.type.raw.first(),
                deviceId.toString(),
                masterData.registerAddress.value.toString(),
                jobId.id
            )
        }
    }.let {
        when (it.status) {
            OK -> {
                logger.info { "Got actual power: '$it'" }
                it.toEnergyValue(masterData)
            }

            else -> {
                logger.warn { "Status: ${it.status}, no actual power received for master data '$masterData' and device id: '$deviceId'." }
                Measurement(now(), 0.0)
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun computeImportedEnergyToday(deviceIds: UByteArray): Measurement<Double> = deviceIds.fold(Measurement(now(), 0.0)) { sum, _ ->
        val measurement = retrieveEnergyTodaySoFar(masterDataForMeterImportedEnergy())
        Measurement(measurement.timestamp, sum.value + measurement.value)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun computeExportedEnergyToday(deviceIds: UByteArray): Measurement<Double> = deviceIds.fold(Measurement(now(), 0.0)) { sum, _ ->
        val measurement = retrieveEnergyTodaySoFar(masterDataForMeterExportedEnergy())
        Measurement(measurement.timestamp, sum.value + measurement.value)
    }

    private suspend fun retrieveEnergyTodaySoFar(masterData: ModbusRegisterDefinition): Measurement<Double> = client.get {
        logger.info { "Getting actual energy for $masterData" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(
                DATA_ACCESSOR_READ_KW_TODAY_SO_FAR_PATH,
                masterData.type.raw.first(),
                masterData.registerAddress.value.toString()
            )
        }
    }.let {
        when (it.status) {
            OK -> {
                logger.info { "Got actual energy: '$it'" }
                it.toEnergyValue(masterData)
            }

            else -> {
                logger.warn { "Status: ${it.status}, no actual energy received for master data '$masterData'." }
                Measurement(now(), 0.0)
            }
        }
    }

}

private fun now(): Long = LocalDateTime.now().toEpochSecond(UTC)

private suspend fun HttpResponse.toEnergyValue(masterData: ModbusRegisterDefinition): Measurement<Double> = body<Measurement<Int>>().let {
    Measurement(it.timestamp, it.value.div(toDoubleOrDefault(masterData.gain.raw, 1.0)))
}

private fun toDoubleOrDefault(text: String, default: Double): Double {
    return when (val value = text.toDoubleOrNull()) {
        null -> default
        else -> value
    }
}
