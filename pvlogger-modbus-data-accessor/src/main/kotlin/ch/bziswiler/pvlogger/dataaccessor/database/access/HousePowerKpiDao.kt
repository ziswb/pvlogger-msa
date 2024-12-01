package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

object HousePowerKpiDao {

    private val logger = KotlinLogging.logger {}

    fun storeAndGetId(kpis: PvLoggerKpiData) = transaction {
        logger.trace { "Storing KPIs" }
        HousePowerKpiTable.insertAndGetId {
            it[jobId] = jobId
            it[solarPower] = kpis.producing
            it[solarToHousePower] = kpis.selfConsuming
            it[gridToHousePower] = kpis.importing
            it[solarToGridPower] = kpis.exporting
            it[ownConsumption] = kpis.consuming
            it[dailyEnergyYield] = kpis.produced
            it[dailyEnergySelfCovered] = kpis.selfConsumed
            it[dailyEnergyExported] = kpis.exported
            it[dailyEnergyImported] = kpis.imported
            it[dailyEnergyConsumed] = kpis.selfConsumed
        }
    }
}