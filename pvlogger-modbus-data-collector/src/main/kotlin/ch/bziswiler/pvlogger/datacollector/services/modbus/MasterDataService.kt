package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.datacollector.configuration.AppConfig
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.ktor.client.request.get
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private const val PARAM_DEV_TYPES = "deviceTypes"
private const val PARAM_CATEGORIES = "categories"
private const val PARAM_VALUE_SEPARATOR = ";"

class MasterDataService(appConfig: AppConfig) {

    private val masterDataHostName = appConfig.masterDataConfig.host
    private val masterDataHostPort = appConfig.masterDataConfig.port

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun fetchRegisterDefinitions(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ): List<ModbusRegisterDefinition> = runBlocking {
        val result = client.get {
            host = masterDataHostName
            port = masterDataHostPort
            url {
                pathSegments = listOf(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES)
                parameters.append(PARAM_DEV_TYPES, deviceTypes.joinToString(separator = PARAM_VALUE_SEPARATOR))
                parameters.append(PARAM_CATEGORIES, categories.joinToString(separator = PARAM_VALUE_SEPARATOR))
            }
        }
        when (result.status) {
            HttpStatusCode.OK -> json.decodeFromString(result.bodyAsText())
            else -> emptyList()
        }
    }
}
