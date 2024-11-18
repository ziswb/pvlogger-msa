package ch.bziswiler.pvlogger.dataaccessor.huawei

class Sun2000Inverter(deviceId: Int, private val activePowerGain: Double, private val dailyEnergyYieldGain: Double) : Device(deviceId) {

    private val activePowerRegisterAddress = 32080 // -> kW
    private val dailyEnergyYieldRegisterAddress = 32114 // -> kWh

    override fun activePowerKW(): Double = activePowerKW(activePowerRegisterAddress, activePowerGain)

    fun dailyEnergyYieldKWh(): Double = activePowerKW(dailyEnergyYieldRegisterAddress, dailyEnergyYieldGain)
}
