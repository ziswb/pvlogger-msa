package ch.bziswiler.pvlogger.dataaccessor.database.access

import kotlinx.serialization.Serializable

@Serializable
data class EnergyToday(
    val exportedEnergyToday: Double = 0.0,
    val importedEnergyToday: Double = 0.0,
    // null while no lifetime-yield baseline exists yet (first day after the
    // V9 rollout) — callers fall back to the device's daily register then.
    val producedEnergyToday: Double? = null,
)
