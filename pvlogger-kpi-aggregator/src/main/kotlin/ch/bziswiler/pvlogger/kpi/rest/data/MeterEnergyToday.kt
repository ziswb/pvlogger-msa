package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toKWh
import ch.bziswiler.pvlogger.shared.rest.Measurement
import ch.bziswiler.pvlogger.shared.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class MeterEnergyToday(private val importedValue: Measurement<Double>, private val importUnit: Unit, private val exportedValue: Measurement<Double>, private val exportUnit: Unit) {
    val timestamp
        get() = importedValue.timestamp
    val imported: KWh
        get() = importUnit.toKWh(importedValue.value)
    val exported: KWh
        get() = exportUnit.toKWh(exportedValue.value)
}