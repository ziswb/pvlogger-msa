package ch.bziswiler.pvlogger.shared.rest

import ch.bziswiler.pvlogger.shared.unit.KW
import ch.bziswiler.pvlogger.shared.unit.KWh
import kotlinx.serialization.Serializable

@Serializable
data class KpiValuesDto(
    val createdAt: Long = 0,
    val solarPower: KW = KW(0.0),
    val gridPower: KW = KW(0.0),
    val solarToHousePower: KW = KW(0.0),
    val gridToHousePower: KW = KW(0.0),
    val solarToGridPower: KW = KW(0.0),
    val ownConsumption: KW = KW(0.0),
    val dailyEnergyYield: KWh = KWh(0.0),
    val dailyEnergySelfCovered: KWh = KWh(0.0),
    val dailyEnergyExported: KWh = KWh(0.0),
    val dailyEnergyImported: KWh = KWh(0.0),
    val dailyEnergyConsumed: KWh = KWh(0.0)
)
