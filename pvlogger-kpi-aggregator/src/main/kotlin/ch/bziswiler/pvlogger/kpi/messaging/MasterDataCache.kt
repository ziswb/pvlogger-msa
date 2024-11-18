package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.rest.PvHttpClient
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import kotlin.time.Duration.Companion.hours

object MasterDataCache {

    private val cache = Cache.Builder<RegisterAddress, ModbusRegisterDefinition>().expireAfterWrite(24.hours).build()
    private val smartMeterRegisterAddress = RegisterAddress(37113)

    suspend fun masterDataForSmartMeter(): ModbusRegisterDefinition = cache.get(smartMeterRegisterAddress) {
        with(getMasterData(smartMeterRegisterAddress)) {
            when (status) {
                HttpStatusCode.OK -> body<ModbusRegisterDefinition>()
                else -> throw Exception("HTTP error: ${status.value} -> ${bodyAsText()}")
            }
        }
    }

    private suspend fun getMasterData(registerAddress: RegisterAddress) = PvHttpClient.client.get {
        url {
            protocol = URLProtocol.HTTP
            host = "pvlogger-masterdata-server-service.pvlogger.svc.cluster.local" // TODO: make this configurable
            port = 8083 // TODO: make this configurable
            pathSegments = listOf("master-data", registerAddress.value.toString())
        }
    }
}