package ch.bziswiler.pvlogger.datacollector.services.modbus

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.masterdata.modbus.Category
import ch.bziswiler.pvlogger.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.masterdata.modbus.dto.ModbusRegisterDefinition
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json


private const val REGISTER_DEFINITIONS_PATH = "/register-definitions"
private const val PARAM_DEV_TYPES = "deviceTypes"
private const val PARAM_CATEGORIES = "categories"
private const val PARAM_VALUE_SEPARATOR = ";"

class MasterDataService(private val appConfig: AppConfig) {

    private val masterDataHostName = appConfig.masterDataConfig.host
    private val masterDataHostPort = appConfig.masterDataConfig.port

    private val client = HttpClient(CIO) {
        install(Logging) { level = LogLevel.ALL }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                allowSpecialFloatingPointValues = true
                encodeDefaults = true
            })
        }
    }

    fun fetchRegisterDefinitions(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ): List<ModbusRegisterDefinition> {
        return runBlocking {
            val result = client.get(REGISTER_DEFINITIONS_PATH) {
                host = masterDataHostName
                port = masterDataHostPort
                url {
                    parameters.append(PARAM_DEV_TYPES, deviceTypes.joinToString(separator = PARAM_VALUE_SEPARATOR))
                    parameters.append(PARAM_CATEGORIES, categories.joinToString(separator = PARAM_VALUE_SEPARATOR))
                }
            }
            if (result.status == HttpStatusCode.OK) {
                return@runBlocking result.body<List<ModbusRegisterDefinition>>()
            } else {
                return@runBlocking emptyList<ModbusRegisterDefinition>()
            }
        }
    }
}
