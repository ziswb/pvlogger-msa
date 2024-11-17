package ch.bziswiler.pvlogger.dataaccessor.services.rest

import kotlinx.serialization.Serializable

@Serializable
data class EnergiesData(
    val selfUsedEnergy: Double,
    val exportedEnergy: Double,
    val importedEnergy: Double,
    val ownConsumptionEnergy: Double
)
