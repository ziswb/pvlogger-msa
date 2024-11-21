package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPathConstants.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import kotlin.time.Duration.Companion.hours

object MasterDataCache {

    private val cache = Cache.Builder<RegisterAddress, ModbusRegisterDefinition>().expireAfterWrite(24.hours).build()

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
            host = "pvlogger-masterdata-server-service.pvlogger.svc.cluster.local"
            port = 8081
            pathSegments = listOf(MASTER_DATA_FOR_ADDRESS_PATH, registerAddress.value.toString())
        }
    }
}