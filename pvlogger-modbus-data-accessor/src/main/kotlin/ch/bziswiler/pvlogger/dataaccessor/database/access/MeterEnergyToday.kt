package ch.bziswiler.pvlogger.dataaccessor.database.access

import kotlinx.serialization.Serializable

@Serializable
data class MeterEnergyToday(
    val exportedEnergyToday: Double = 0.0,
    val importedEnergyToday: Double = 0.0,
)
