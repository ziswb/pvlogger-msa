package ch.bziswiler.pvlogger.dataaccessor.database.access

import kotlinx.serialization.Serializable

@Serializable
data class EnergyToday(
    val exportedEnergyToday: Double = 0.0,
    val importedEnergyToday: Double = 0.0,
    // null only while the raw-value store is completely empty: a missing
    // lifetime-yield baseline (e.g. first day after a fresh rollout) is
    // covered by the daily-register fallback inside the endpoint (ADR-0014).
    val producedEnergyToday: Double? = null,
)
