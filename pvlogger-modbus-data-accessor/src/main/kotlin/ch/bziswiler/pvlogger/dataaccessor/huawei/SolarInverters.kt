package ch.bziswiler.pvlogger.business.model.huawei.device

class SolarInverters(deviceId: Int = 0, private val inverters: List<Sun2000Inverter>) : Device(deviceId) {

    override fun activePowerKW(): Double = inverters.sumOf { it.activePowerKW() }

    fun dailyEnergyYieldKWh(): Double = inverters.sumOf { it.dailyEnergyYieldKWh() }
}
