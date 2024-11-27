package ch.bziswiler.pvlogger.kpi.messaging

import ch.bziswiler.pvlogger.kpi.rest.InverterActivePower
import ch.bziswiler.pvlogger.kpi.rest.MeterActivePower

data class KpiLiveData(private val inverterActivePower: InverterActivePower, private val meterActivePower: MeterActivePower) {
    val producing = inverterActivePower.producing
    val exporting = meterActivePower.exporting
    val importing = meterActivePower.importing
    val consuming = producing - exporting + importing
    val selfConsuming = producing - exporting
}
