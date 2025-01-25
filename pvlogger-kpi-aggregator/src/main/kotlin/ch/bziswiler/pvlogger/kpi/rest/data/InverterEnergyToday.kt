package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toKWh
import ch.bziswiler.pvlogger.shared.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(private val producedValue: Double, private val sourceUnit: Unit) {
    val produced: KWh
        get() = sourceUnit.toKWh(producedValue)
}
