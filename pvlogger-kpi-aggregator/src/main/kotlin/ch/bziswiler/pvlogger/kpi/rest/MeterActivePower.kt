package ch.bziswiler.pvlogger.kpi.rest

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class MeterActivePower(private val power: Double) {
    val exporting: Double
        get() = if (power > 0) {
            power
        } else {
            0.0
        }
    val importing: Double
        get() = if (power < 0) {
            abs(power)
        } else {
            0.0
        }
}
