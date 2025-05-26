package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toW
import ch.bziswiler.pvlogger.shared.rest.Measurement
import ch.bziswiler.pvlogger.shared.unit.W
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class MeterActivePower(private val power: Measurement<Double>, private val sourceUnit: Unit) {
    val timestamp
        get() = power.timestamp
    val exporting: W
        get() = if (power.value > 0) {
            sourceUnit.toW(power.value)
        } else {
            sourceUnit.toW(0.0)
        }
    val importing: W
        get() = if (power.value < 0) {
            sourceUnit.toW(abs(power.value))
        } else {
            sourceUnit.toW(0.0)
        }
}
