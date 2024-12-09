package ch.bziswiler.pvlogger.shared.rest

import kotlinx.serialization.Serializable

@Serializable
data class KpiValuesDto(
    val createdAt: Long = 0,
    val solarPower: Double = 0.0,
    val gridPower: Double = 0.0,
    val solarToHousePower: Double = 0.0,
    val gridToHousePower: Double = 0.0,
    val solarToGridPower: Double = 0.0,
    val dailyEnergyYield: Double = 0.0,
    val ownConsumption: Double = 0.0,
    val dailyEnergySelfCovered: Double = 0.0,
    val dailyEnergyExported: Double = 0.0,
    val dailyEnergyImported: Double = 0.0,
    val dailyEnergyConsumed: Double = 0.0
)
