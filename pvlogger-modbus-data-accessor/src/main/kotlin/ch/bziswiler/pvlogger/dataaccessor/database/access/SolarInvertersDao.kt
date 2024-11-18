package ch.bziswiler.pvlogger.dataaccessor.database.access

class SolarInvertersDao(deviceId: Int = 0, private val inverters: List<Sun2000InverterDao>) : DeviceDao(deviceId) {

    override fun activePowerKW(): Double = inverters.sumOf { it.activePowerKW() }

    fun dailyEnergyYieldKWh(): Double = inverters.sumOf { it.dailyEnergyYieldKWh() }
}
