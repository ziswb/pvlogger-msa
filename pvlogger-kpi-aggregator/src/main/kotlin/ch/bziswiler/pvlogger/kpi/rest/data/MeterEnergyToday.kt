package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toKWh
import ch.bziswiler.pvlogger.shared.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class MeterEnergyToday(private val importedValue: Double, private val importUnit: Unit, private val exportedValue: Double, private val exportUnit: Unit) {
    val imported: KWh
        get() = importUnit.toKWh(importedValue)
    val exported: KWh
        get() = exportUnit.toKWh(exportedValue)
}