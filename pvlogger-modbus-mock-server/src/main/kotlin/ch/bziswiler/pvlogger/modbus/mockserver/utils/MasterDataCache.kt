package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.shared.ktor.PvHttpClient.client
import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import kotlin.time.Duration.Companion.hours

object MasterDataCache {

    private var host = "localhost"
    private var port = 8081

    private val cache = Cache.Builder<RegisterAddress, ModbusRegisterDefinition>().expireAfterWrite(24.hours).build()

    fun configure(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    suspend fun modbusRegisterDefinition(registerAddress: RegisterAddress) = cache.get(registerAddress) {
        with(getMasterData(registerAddress)) {
            when (status) {
                HttpStatusCode.OK -> body<ModbusRegisterDefinition>()
                else -> throw Exception("HTTP error: ${status.value} -> ${bodyAsText()}")
            }
        }
    }

    private suspend fun getMasterData(registerAddress: RegisterAddress) = client.get {
        url {
            protocol = URLProtocol.HTTP
            host = MasterDataCache.host
            port = MasterDataCache.port
            pathSegments = listOf(MASTER_DATA_FOR_ADDRESS_PATH, registerAddress.value.toString())
        }
    }
}