package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyConsumed
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyExported
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyImported
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyYield
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dateCreated
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.gridToHousePower
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.ownConsumption
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.solarPower
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.solarToGridPower
import ch.bziswiler.pvlogger.shared.configuration.TimeZones.LOCAL_ZONE
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder.ASC
import org.jetbrains.exposed.v1.core.SortOrder.DESC
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import ch.bziswiler.pvlogger.shared.database.dbTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.now

private const val TWO_DECIMALS = "#.##"

object HousePowerKpiDao {

    private val logger = KotlinLogging.logger {}
    private val rounder = DecimalFormat(TWO_DECIMALS)

    init {
        rounder.roundingMode = RoundingMode.HALF_UP
    }

    suspend fun store(kpis: PvLoggerKpiData) = dbTransaction {
        logger.trace { "Storing KPIs '$kpis'" }
        HousePowerKpiTable.insert {
            it[jobId] = kpis.jobId
            it[dateCreated] = Instant.ofEpochMilli(kpis.dateCreated)
            it[solarPower] = kpis.producing.toKW().kw
            it[solarToHousePower] = kpis.selfConsuming.toKW().kw
            it[gridToHousePower] = kpis.importing.toKW().kw
            it[solarToGridPower] = kpis.exporting.toKW().kw
            it[ownConsumption] = kpis.consuming.toKW().kw
            it[dailyEnergyYield] = kpis.produced.toKWh().kWh
            it[dailyEnergySelfCovered] = kpis.selfConsumed.toKWh().kWh
            it[dailyEnergyExported] = kpis.exported.toKWh().kWh
            it[dailyEnergyImported] = kpis.imported.toKWh().kWh
            it[dailyEnergyConsumed] = kpis.consumed.toKWh().kWh
        }
    }

    suspend fun todaysKpis() = dbTransaction {
        logger.info { "getting today's KPIs" }
        val todayStart = now(LOCAL_ZONE).atStartOfDay(LOCAL_ZONE).toInstant()
        HousePowerKpiTable
            .selectAll()
            .where { dateCreated.greaterEq(todayStart) }
            .orderBy(dateCreated, DESC)
            .map { toKpiValuesDto(it) }
            .toList()
    }

    suspend fun latestKpi() = dbTransaction {
        logger.info { "getting latest KPI" }
        HousePowerKpiTable
            .selectAll()
            .orderBy(dateCreated, DESC)
            .limit(1)
            .firstOrNull()
            ?.let { toKpiValuesDto(it) }
    }

    suspend fun kpisOf(date: LocalDate) = dbTransaction {
        logger.info { "getting KPIs of date '$date'" }
        val dayStart = date.atStartOfDay(LOCAL_ZONE).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(LOCAL_ZONE).toInstant()
        HousePowerKpiTable
            .selectAll()
            .where {
                dateCreated
                    .greaterEq(dayStart)
                    .and(dateCreated.less(dayEnd))
            }
            .orderBy(dateCreated, ASC)
            .map { toKpiValuesDto(it) }
            .toList()
    }

    private fun toKpiValuesDto(it: ResultRow) = KpiValuesDto(
        createdAt = it[dateCreated].toEpochMilli(),
        solarPower = rounder.format(it[solarPower]).toDouble(),
        gridPower = rounder.format(it[gridToHousePower].minus(it[solarToGridPower])).toDouble(),
        solarToHousePower = rounder.format(it[HousePowerKpiTable.solarToHousePower]).toDouble(),
        gridToHousePower = rounder.format(it[gridToHousePower]).toDouble(),
        solarToGridPower = rounder.format(it[solarToGridPower]).toDouble(),
        ownConsumption = rounder.format(it[ownConsumption]).toDouble(),
        dailyEnergyYield = rounder.format(it[dailyEnergyYield]).toDouble(),
        dailyEnergySelfCovered = rounder.format(it[HousePowerKpiTable.dailyEnergySelfCovered]).toDouble(),
        dailyEnergyExported = rounder.format(it[dailyEnergyExported]).toDouble(),
        dailyEnergyImported = rounder.format(it[dailyEnergyImported]).toDouble(),
        dailyEnergyConsumed = rounder.format(it[dailyEnergyConsumed]).toDouble()
    )
}
