package ch.bziswiler.pvlogger.shared.messaging

import java.time.LocalDateTime

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
@kotlinx.serialization.Serializable
data class PvLoggerKpiData(
    val jobId: String,
    val dateCreated: Long,
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
) : java.io.Serializable
