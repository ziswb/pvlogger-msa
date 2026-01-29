package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.shared.unit.W
import ch.bziswiler.pvlogger.shared.unit.Wh
import kotlinx.serialization.Serializable

/**
 * Was formerly:
 *
 * {@code
 * val solarToHousePower: Double = selfConsuming
 * val gridToHousePower: Double = importing
 * val solarToGridPower: Double = exporting
 * val solarPower: Double = producing
 * val ownConsumption: Double = consuming
 * val dailyEnergyYield: Double = produced
 * val dailyEnergyExported: Double = exported
 * val dailyEnergyImported: Double = imported
 * val dailyEnergyConsumed: Double = consumed
 * val dailyEnergySelfCovered: Double = selfConsumed
 * }
 */
@Serializable
data class PvLoggerKpiData(
    val jobId: String,
    val dateCreated: Long,
    val producing: W,
    val exporting: W,
    val importing: W,
    val consuming: W,
    val selfConsuming: W,
    val produced: Wh,
    val exported: Wh,
    val imported: Wh,
    val consumed: Wh,
    val selfConsumed: Wh,
)