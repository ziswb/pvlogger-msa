package ch.bziswiler.pvlogger.kpi.messaging.data

import ch.bziswiler.pvlogger.kpi.rest.data.InverterActivePower
import ch.bziswiler.pvlogger.kpi.rest.data.MeterActivePower

data class KpiLiveData(private val inverterActivePower: InverterActivePower, private val meterActivePower: MeterActivePower) {
    val producing = inverterActivePower.producing
    val exporting = meterActivePower.exporting
    val importing = meterActivePower.importing
    val consuming = producing - exporting + importing
    val selfConsuming = producing - exporting

    override fun toString(): String =
        """
            val producing = $producing
            val exporting = $exporting
            val importing = $importing
            val consuming = $consuming
            val selfConsuming = $selfConsuming
        """.trimIndent()
}
