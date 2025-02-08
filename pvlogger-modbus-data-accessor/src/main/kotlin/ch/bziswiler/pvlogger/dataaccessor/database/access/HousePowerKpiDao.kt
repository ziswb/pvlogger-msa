package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime.ofInstant
import java.time.ZoneOffset.UTC

object HousePowerKpiDao {

    private val logger = KotlinLogging.logger {}

    fun store(kpis: PvLoggerKpiData) = transaction {
        logger.trace { "Storing KPIs '$kpis'" }
        HousePowerKpiTable.insert {
            it[jobId] = kpis.jobId
            it[dateCreated] = ofInstant(ofEpochMilli(kpis.dateCreated), UTC)
            it[solarPower] = kpis.producing.toKW().kw
            it[solarToHousePower] = kpis.selfConsuming.toKW().kw
            it[gridToHousePower] = kpis.importing.toKW().kw
            it[solarToGridPower] = kpis.exporting.toKW().kw
            it[ownConsumption] = kpis.consuming.toKW().kw
            it[dailyEnergyYield] = kpis.produced.toKWh().kWh
            it[dailyEnergySelfCovered] = kpis.selfConsumed.toKWh().kWh
            it[dailyEnergyExported] = kpis.exported.toKWh().kWh
            it[dailyEnergyImported] = kpis.imported.toKWh().kWh
            it[dailyEnergyConsumed] = kpis.selfConsumed.toKWh().kWh
        }
    }
}