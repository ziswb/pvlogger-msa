package ch.bziswiler.pvlogger.datacollector.services.rest

import ch.bziswiler.pvlogger.datacollector.configuration.MasterDataConfig
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_DEVICE_ID_VALUES
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_MODBUS_VALUES
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZonedDateTime

@Serializable
data class DataBatch(
    @Contextual val timestampIso: ZonedDateTime,
    val record: ModbusResponse,
)

private val json =
    Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

private val myClient = OkHttpClient()

class StoreModbusValuesMessageSender(
    private val config: MasterDataConfig,
) {
    fun sendValuesToStore(data: ModbusResponse) {
        val batch =
            DataBatch(
                timestampIso = ZonedDateTime.now(),
                record = data,
            )

        when (data) {
            is ModbusRegisterValueResponse -> {
                sendBatch(batch, DATA_ACCESSOR_STORE_MODBUS_VALUES)
            }

            is ReadDeviceIdentifiersResponse -> {
                sendBatch(batch, DATA_ACCESSOR_STORE_DEVICE_ID_VALUES)
            }
        }
    }

    private fun sendBatch(
        batch: DataBatch,
        targetUrl: String,
    ) {
        val body = json.encodeToString<DataBatch>(batch).toRequestBody("application/json".toMediaType())
        val path = "http://${config.host}:${config.port}/${DATA_ACCESSOR_API_V1_PATH}/$targetUrl"
        val request =
            Request
                .Builder()
                .url(path)
                .post(body)
                .build()

        myClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to send batch: HTTP ${response.code} - ${response.message}")
            }
        }
    }
}
