package ch.bziswiler.pvlogger.datacollector.services.rest

import ch.bziswiler.pvlogger.datacollector.configuration.MasterDataConfig
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

private const val PARAM_DEV_TYPES = "deviceTypes"
private const val PARAM_CATEGORIES = "categories"
private const val PARAM_VALUE_SEPARATOR = ";"

class MasterDataService(config: MasterDataConfig) {

    private val masterDataHostName = config.host
    private val masterDataHostPort = config.port
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://$masterDataHostName:$masterDataHostPort/")
        .client(okHttpClient)
        .build()

    private val api: MasterDataApi = retrofit.create(MasterDataApi::class.java)

    private interface MasterDataApi {
        @GET(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES)
        suspend fun getMasterData(
            @Query(PARAM_DEV_TYPES) deviceTypes: String,
            @Query(PARAM_CATEGORIES) categories: String,
        ): ResponseBody
    }

    fun fetchRegisterDefinitions(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ): List<ModbusRegisterDefinition> = runBlocking {
        val devTypesParam = deviceTypes.joinToString(separator = PARAM_VALUE_SEPARATOR)
        val categoriesParam = categories.joinToString(separator = PARAM_VALUE_SEPARATOR)
        val body = api.getMasterData(devTypesParam, categoriesParam)
        val text = body.string()
        json.decodeFromString(ListSerializer(ModbusRegisterDefinition.serializer()), text)
    }
}
