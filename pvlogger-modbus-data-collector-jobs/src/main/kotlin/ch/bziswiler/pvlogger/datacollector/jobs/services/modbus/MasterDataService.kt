package ch.bziswiler.pvlogger.datacollector.jobs.services.modbus

import ch.bziswiler.pvlogger.datacollector.jobs.configuration.AppConfig
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

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

    private val api: MasterDataApi by lazy {
        val baseUrl = "http://$masterDataHostName:$masterDataHostPort/"
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .build()
            .create(MasterDataApi::class.java)
    }

    fun fetchRegisterDefinitions(
        deviceTypes: List<DeviceType>, categories: List<Category>
    ): List<ModbusRegisterDefinition> = runBlocking {
        val devTypes = deviceTypes.joinToString(separator = PARAM_VALUE_SEPARATOR)
        val cats = categories.joinToString(separator = PARAM_VALUE_SEPARATOR)
        val call: Call<ResponseBody> = api.getRegisterDefinitions(devTypes, cats)
        val response: Response<ResponseBody> = call.execute()
        if (response.isSuccessful) {
            val body = response.body()?.string()
            if (body != null) json.decodeFromString(body) else emptyList()
        } else {
            emptyList()
        }
    }
}

private interface MasterDataApi {
    @GET(MASTER_DATA_FOR_CATEGORIES_AND_DEVICE_TYPES)
    fun getRegisterDefinitions(
        @Query(PARAM_DEV_TYPES) deviceTypes: String,
        @Query(PARAM_CATEGORIES) categories: String
    ): Call<ResponseBody>
}
