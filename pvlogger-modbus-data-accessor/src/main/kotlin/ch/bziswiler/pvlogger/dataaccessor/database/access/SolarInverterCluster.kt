package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.shared.masterdata.unit.KW
import ch.bziswiler.pvlogger.shared.masterdata.unit.KWh

class SolarInverterCluster(private val inverters: List<SolarInverterDao>) {

    fun activePowerKW(): KW = inverters.map { it.activePowerKW() }.fold(KW(0.0)) { sum: KW, value: KW -> sum + value }

    fun dailyEnergyYieldKWh(): KWh = inverters.map { it.dailyEnergyYieldKWh() }.fold((KWh(0.0))) { sum: KWh, value: KWh -> sum + value }
}
