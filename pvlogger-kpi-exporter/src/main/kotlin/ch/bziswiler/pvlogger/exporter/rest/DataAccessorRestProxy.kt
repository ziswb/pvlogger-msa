package ch.bziswiler.pvlogger.exporter.rest

import ch.bziswiler.pvlogger.exporter.configuration.ModbusDataAccessConfig
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_VERSION_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET

class DataAccessorRestProxy(
    private val config: ModbusDataAccessConfig,
) {
    private val logger = KotlinLogging.logger {}

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val api: KpiValuesApi by lazy {
        val baseUrl = "http://${config.host}:${config.port}"
        Retrofit.Builder().baseUrl(baseUrl).build().create(KpiValuesApi::class.java)
    }

    private interface KpiValuesApi {
        @GET("${DATA_ACCESSOR_READ_DATA_VERSION_PATH}/${DATA_ACCESSOR_READ_LATEST_KPI_PATH}")
        fun retrieveKpiValues(): Call<ResponseBody>
    }

    fun retrieveLatestKpis(): KpiValuesDto? {
        return runBlocking {
            logger.info { "Getting latest KPIs" }
            val response: Response<ResponseBody> = api.retrieveKpiValues().execute()
            if (response.isSuccessful && response.body() != null) {
                logger.info { "Got latest KPIs: '$response'" }
                json.decodeFromString<KpiValuesDto>(response.body()!!.string())
            } else {
                logger.warn { "Status: ${response.code()}, no KPIs found." }
                null
            }
        }
    }
}
