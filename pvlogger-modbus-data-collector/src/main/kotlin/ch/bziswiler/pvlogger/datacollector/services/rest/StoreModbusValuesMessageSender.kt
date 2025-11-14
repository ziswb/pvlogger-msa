package ch.bziswiler.pvlogger.datacollector.services.rest

import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
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

private val myclient = OkHttpClient()

class StoreModbusValuesMessageSender {
    fun sendValuesToStore(data: ModbusResponse) {
        val batch =
            DataBatch(
                timestampIso = ZonedDateTime.now(),
                record = data,
            )

        val targetUrl =
            System.getenv("PERSIST_ENDPOINT") ?: "http://data-persist.default.svc.cluster.local/v1/data-batch"

        sendBatch(batch, targetUrl)
    }

    private fun sendBatch(
        batch: DataBatch,
        targetUrl: String,
    ) {
        val jsonString: String = json.encodeToString(batch)

        val body = jsonString.toRequestBody("application/json".toMediaType())

        val request =
            Request
                .Builder()
                .url(targetUrl)
                .post(body)
                .build()

        myclient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to send batch: HTTP ${response.code} - ${response.message}")
            }
        }
    }
}
