package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.unit.KWh

class SolarInverterDao(
    deviceId: Int,
    activePowerRegisterAddress: RegisterAddress,
    override val activePowerGain: Gain,
    private val dailyEnergyYieldRegisterAddress: RegisterAddress,
    private val dailyEnergyYieldGain: Gain
) : SolarDeviceDao(deviceId, activePowerRegisterAddress) {

//    private val inverterActivePowerRegisterAddress = RegisterAddress(32080) // -> kW
//    private val inverterDailyEnergyYieldRegisterAddress = RegisterAddress(32114) // -> kWh

    fun dailyEnergyYieldKWh(): KWh = readAndTransposeLatestRegisterValue(dailyEnergyYieldRegisterAddress, dailyEnergyYieldGain).toKWh()
}
