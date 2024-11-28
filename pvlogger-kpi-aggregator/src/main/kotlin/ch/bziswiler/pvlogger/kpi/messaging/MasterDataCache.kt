package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.configuration.AppConfig
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import ch.bziswiler.pvlogger.shared.rest.MasterDataPaths.MASTER_DATA_FOR_ADDRESS_PATH
import ch.bziswiler.pvlogger.shared.rest.PvHttpClient.client
import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import org.koin.java.KoinJavaComponent.inject
import kotlin.time.Duration.Companion.hours

object MasterDataCache {

    private val cache = Cache.Builder<RegisterAddress, ModbusRegisterDefinition>().expireAfterWrite(24.hours).build()
    private val smartMeterActivePowerRegisterAddress = RegisterAddress(37113) // -> W
    private val inverterActivePowerRegisterAddress = RegisterAddress(32080) // -> kW
    private val smartMeterExportedEnergyRegisterAddress = RegisterAddress(37119) // -> kWh
    private val smartMeterImportedEnergyRegisterAddress = RegisterAddress(37121) // -> kWh
    private val inverterDailyEnergyYieldRegisterAddress = RegisterAddress(32114) // -> kWh

    suspend fun masterDataForSmartMeterActivePower(): ModbusRegisterDefinition = modbusRegisterDefinition(smartMeterActivePowerRegisterAddress)

    suspend fun masterDataForInverterActivePower(): ModbusRegisterDefinition = modbusRegisterDefinition(inverterActivePowerRegisterAddress)

    suspend fun masterDataForInverterEnergyYield(): ModbusRegisterDefinition = modbusRegisterDefinition(inverterDailyEnergyYieldRegisterAddress)

    suspend fun masterDataForMeterExportedEnergy(): ModbusRegisterDefinition = modbusRegisterDefinition(smartMeterExportedEnergyRegisterAddress)

    suspend fun masterDataForMeterImportedEnergy(): ModbusRegisterDefinition = modbusRegisterDefinition(smartMeterImportedEnergyRegisterAddress)

    private suspend fun modbusRegisterDefinition(registerAddress: RegisterAddress) = cache.get(registerAddress) {
        with(getMasterData(registerAddress)) {
            when (status) {
                HttpStatusCode.OK -> body<ModbusRegisterDefinition>()
                else -> throw Exception("HTTP error: ${status.value} -> ${bodyAsText()}")
            }
        }
    }

    private suspend fun getMasterData(registerAddress: RegisterAddress) = client.get {
        val appConfig: AppConfig by inject(AppConfig::class.java)
        url {
            protocol = URLProtocol.HTTP
            host = appConfig.masterDataConfig.host
            port = appConfig.masterDataConfig.port
            pathSegments = listOf(MASTER_DATA_FOR_ADDRESS_PATH, registerAddress.value.toString())
        }
    }
}