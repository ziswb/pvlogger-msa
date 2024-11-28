package ch.bziswiler.pvlogger.kpi.rest

import kotlinx.serialization.Serializable

@Serializable
data class InverterEnergyToday(private val power: Double) {
    val produced: Double
        get() = power
}
