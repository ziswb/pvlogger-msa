package ch.bziswiler.pvlogger.kpi.rest

import kotlin.math.abs

data class MeterEnergyToday(val power: Double) {
    val exported: Double
        get() = if (power > 0) {
            power
        } else {
            0.0
        }
    val imported: Double
        get() = if (power < 0) {
            abs(power)
        } else {
            0.0
        }
}
