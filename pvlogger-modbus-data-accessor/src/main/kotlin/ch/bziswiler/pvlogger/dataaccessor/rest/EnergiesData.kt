package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.shared.masterdata.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class EnergiesData(
    val selfUsedEnergy: KWh,
    val exportedEnergy: KWh,
    val importedEnergy: KWh,
    val ownConsumptionEnergy: KWh
)
