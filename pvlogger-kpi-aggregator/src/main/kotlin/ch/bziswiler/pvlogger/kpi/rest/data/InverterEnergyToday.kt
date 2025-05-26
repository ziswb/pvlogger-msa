package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toKWh
import ch.bziswiler.pvlogger.shared.rest.Measurement
import ch.bziswiler.pvlogger.shared.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(private val producedValue: Measurement<Double>, private val sourceUnit: Unit) {
    val timestamp
        get() = producedValue.timestamp
    val produced: KWh
        get() = sourceUnit.toKWh(producedValue.value)
}
