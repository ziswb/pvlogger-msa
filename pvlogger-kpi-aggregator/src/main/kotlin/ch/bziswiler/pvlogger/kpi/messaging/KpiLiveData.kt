package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.rest.dto.InverterActivePower
import ch.bziswiler.pvlogger.kpi.rest.dto.MeterActivePower

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
