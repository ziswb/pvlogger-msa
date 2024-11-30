package ch.bziswiler.pvlogger.kpi.messaging.data

import ch.bziswiler.pvlogger.kpi.rest.data.InverterEnergyToday
import ch.bziswiler.pvlogger.kpi.rest.data.MeterEnergyToday

data class KpiTodayData(private val inverterTodayEnergy: InverterEnergyToday, private val meterTodayEnergy: MeterEnergyToday) {
    val produced = inverterTodayEnergy.produced
    val exported = meterTodayEnergy.exported
    val imported = meterTodayEnergy.imported
    val consumed = produced - exported + imported
    val selfConsumed = produced - exported

    override fun toString(): String =
        """
            val produced = $produced
            val exported = $exported
            val imported = $imported
            val consumed = $consumed
            val selfConsumed = $selfConsumed
        """.trimIndent()
}
