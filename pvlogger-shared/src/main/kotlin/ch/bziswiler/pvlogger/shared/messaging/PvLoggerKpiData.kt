package ch.bziswiler.pvlogger.shared.messaging

import kotlinx.serialization.Serializable

@Serializable
data class PvLoggerKpiData(
    val timestamp: Long,
    val producing: Double,
    val exporting: Double,
    val importing: Double,
    val consuming: Double,
    val selfConsuming: Double,
    val produced: Double,
    val exported: Double,
    val imported: Double,
    val consumed: Double,
    val selfConsumed: Double
)
