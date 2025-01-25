package ch.bziswiler.pvlogger.kpi.rest.data

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import ch.bziswiler.pvlogger.shared.masterdata.modbus.toW
import ch.bziswiler.pvlogger.shared.unit.W
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class MeterActivePower(private val power: Double, private val sourceUnit: Unit) {
    val exporting: W
        get() = if (power > 0) {
            sourceUnit.toW(power)
        } else {
            sourceUnit.toW(0.0)
        }
    val importing: W
        get() = if (power < 0) {
            sourceUnit.toW(abs(power))
        } else {
            sourceUnit.toW(0.0)
        }
}
