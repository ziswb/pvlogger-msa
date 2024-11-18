package ch.bziswiler.pvlogger.dataaccessor.database.access

class Sun2000InverterDao(deviceId: Int, private val activePowerGain: Double, private val dailyEnergyYieldGain: Double) : DeviceDao(deviceId) {

    private val activePowerRegisterAddress = 32080 // -> kW
    private val dailyEnergyYieldRegisterAddress = 32114 // -> kWh

    override fun activePowerKW(): Double = readAndTransposeLatestRegisterValue(activePowerRegisterAddress, activePowerGain)

    fun dailyEnergyYieldKWh(): Double = readAndTransposeLatestRegisterValue(dailyEnergyYieldRegisterAddress, dailyEnergyYieldGain)
}
