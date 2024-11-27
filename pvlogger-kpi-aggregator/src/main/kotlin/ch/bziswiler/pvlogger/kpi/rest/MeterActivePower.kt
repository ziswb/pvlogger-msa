package ch.bziswiler.pvlogger.kpi.rest

import kotlin.math.abs

data class MeterActivePower(val power: Double) {
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
