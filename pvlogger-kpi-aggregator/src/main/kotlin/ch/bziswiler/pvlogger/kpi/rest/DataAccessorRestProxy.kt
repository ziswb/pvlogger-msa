package ch.bziswiler.pvlogger.kpi.rest

import ch.bziswiler.pvlogger.kpi.configuration.ModbusDataAccessConfig
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForInverterActivePower
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForInverterEnergyYield
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForMeterExportedEnergy
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForMeterImportedEnergy
import ch.bziswiler.pvlogger.kpi.messaging.MasterDataCache.masterDataForSmartMeterActivePower
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_TODAY_SO_FAR_REGISTER_VALUE_PATH
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode.Companion.OK
import mu.KotlinLogging

class DataAccessorRestProxy(private val config: ModbusDataAccessConfig) {

    internal val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun inverterActivePower() = InverterActivePower(retrieveLatestRegisterValuesAggregated(masterDataForInverterActivePower(), config.inverterDeviceIds))

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun meterActivePower() = MeterActivePower(retrieveLatestRegisterValuesAggregated(masterDataForSmartMeterActivePower(), config.meterDeviceIds))

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun inverterEnergyToday() = InverterEnergyToday(retrieveLatestRegisterValuesAggregated(masterDataForInverterEnergyYield(), config.inverterDeviceIds))

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun meterEnergyToday() = MeterEnergyToday(computeImportedEnergyToday(config.meterDeviceIds), computeExportedEnergyToday(config.meterDeviceIds))

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun retrieveLatestRegisterValuesAggregated(masterData: ModbusRegisterDefinition, deviceIds: UByteArray) = deviceIds.fold(0.0) { sum, deviceId ->
        sum + retrieveLatestRegisterValue(masterData, deviceId)
    }

    private suspend fun retrieveLatestRegisterValue(masterData: ModbusRegisterDefinition, deviceId: UByte): Double = client.get {
        logger.info { "Getting actual power for $masterData" }
        url {
            host = config.host
            port = config.port
            pathSegments = listOf(
                DATA_ACCESSOR_READ_LATEST_REGISTER_VALUE_PATH,
                masterData.type.raw.first(),
                deviceId.toString(),
                masterData.registerAddress.value.toString()
            )
        }
    }.let {
        when (it.status) {
            OK -> {
                logger.info { "Got actual power: '$it'" }
                val text = it.bodyAsText()
                when (text.isNotBlank()) {
                    true -> text.toInt().div(masterData.gain.raw.toInt()).toDouble()
                    else -> 0.0.also { logger.debug { "Got no actual power! Returning '0.0'." } }
                }
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
                val text = it.bodyAsText()
                when (text.isNotBlank()) {
                    true -> text.toInt().div(masterData.gain.raw.toInt()).toDouble()
                    else -> 0.0
                }
            }

            else -> {
                logger.warn { "Status: ${it.status}, no actual energy received for master data '$masterData'." }
                0.0
            }
        }
    }
}
