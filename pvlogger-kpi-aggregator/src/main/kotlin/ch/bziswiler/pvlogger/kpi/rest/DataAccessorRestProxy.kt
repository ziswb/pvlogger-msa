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
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.OK
import mu.KotlinLogging

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
    private suspend fun retrieveLatestRegisterValuesAggregated(masterData: ModbusRegisterDefinition, deviceIds: UByteArray, jobId: JobId) = deviceIds.fold(0.0) { sum, deviceId ->
        sum + retrieveLatestRegisterValue(masterData, deviceId, jobId)
    }

    private suspend fun retrieveLatestRegisterValue(masterData: ModbusRegisterDefinition, deviceId: UByte, jobId: JobId): Double = client.get {
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
                0.0
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun computeImportedEnergyToday(deviceIds: UByteArray): Double = deviceIds.fold(0.0) { sum, _ ->
        sum + retrieveEnergyTodaySoFar(masterDataForMeterImportedEnergy())
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun computeExportedEnergyToday(deviceIds: UByteArray): Double = deviceIds.fold(0.0) { sum, _ ->
        sum + retrieveEnergyTodaySoFar(masterDataForMeterExportedEnergy())
    }

    private suspend fun retrieveEnergyTodaySoFar(masterData: ModbusRegisterDefinition): Double = client.get {
        logger.info { "Getting actual energy for $masterData" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(
                DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH,
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
                0.0
            }
        }
    }

}

private suspend fun HttpResponse.toEnergyValue(masterData: ModbusRegisterDefinition): Double = toDoubleOrDefault(bodyAsText(), 0.0).div(toDoubleOrDefault(masterData.gain.raw, 1.0))

private fun toDoubleOrDefault(text: String, default: Double): Double {
    return when (val value = text.toDoubleOrNull()) {
        null -> default
        else -> value
    }
}
