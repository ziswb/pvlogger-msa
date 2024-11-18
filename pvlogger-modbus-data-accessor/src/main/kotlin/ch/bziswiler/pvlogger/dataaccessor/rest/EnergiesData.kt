package ch.bziswiler.pvlogger.dataaccessor.rest

import kotlinx.serialization.Serializable

@Serializable
data class EnergiesData(
    val selfUsedEnergy: Double,
    val exportedEnergy: Double,
    val importedEnergy: Double,
    val ownConsumptionEnergy: Double
)
