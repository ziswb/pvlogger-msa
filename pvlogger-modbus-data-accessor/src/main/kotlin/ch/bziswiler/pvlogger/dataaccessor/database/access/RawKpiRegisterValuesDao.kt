package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.RawKpiRegisterValuesTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object RawKpiRegisterValuesDao {

    private val logger = KotlinLogging.logger {}

    fun store(values: RawKpiRegisterValues) = transaction {
        logger.trace { "Storing raw KPI register values '$values'" }
        RawKpiRegisterValuesTable.insert {
            it[jobId] = values.jobId
            it[inverterActivePower] = values.inverterActivePower
            it[meterActivePower] = values.meterActivePower
            it[inverterDailyYield] = values.inverterDailyYield
            it[inverterAccumulatedYield] = values.inverterAccumulatedYield
            it[meterExportedEnergy] = values.meterExportedEnergy
            it[meterImportedEnergy] = values.meterImportedEnergy
        }
    }

    private enum class EnergyColumn(val dbColumn: String, val zeroIsNoData: Boolean = false) {
        EXPORTED("meter_exported_energy"),
        IMPORTED("meter_imported_energy"),
        // Pre-V9 rows carry 0.0 in this column; a real lifetime counter is
        // never 0, so 0 is treated as "no data" to avoid a bogus baseline.
        PRODUCED("inverter_accumulated_yield", zeroIsNoData = true),
    }

    fun energyToday(): EnergyToday = transaction {
        logger.info { "computing energy today" }
        EnergyToday(
            exportedEnergyToday = computeEnergyDelta(EnergyColumn.EXPORTED) ?: 0.0,
            importedEnergyToday = computeEnergyDelta(EnergyColumn.IMPORTED) ?: 0.0,
            producedEnergyToday = computeEnergyDelta(EnergyColumn.PRODUCED) ?: newestDailyYield(),
        )
    }

    // Fallback while the lifetime counter has no yesterday baseline
    // (ADR-0014): the newest row's daily-yield register — the value the
    // collector stored just before asking. Null only when the store is empty.
    // Lives server-side so the single KPI aggregation path (ADR-0016) needs
    // no register knowledge of its own for the daily figures.
    private fun newestDailyYield(): Double? =
        RawKpiRegisterValuesTable
            .select(RawKpiRegisterValuesTable.inverterDailyYield)
            .orderBy(RawKpiRegisterValuesTable.dateCreated to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.get(RawKpiRegisterValuesTable.inverterDailyYield)

    private fun computeEnergyDelta(column: EnergyColumn): Double? {
        val kWhColumn = 2
        val valueExpr = if (column.zeroIsNoData) "NULLIF(r.${column.dbColumn}, 0)" else "r.${column.dbColumn}"
        // "Today" is anchored on the newest raw row (the caller stores its raw
        // values before asking for the delta), not on CURRENT_DATE — so a
        // collection cycle straddling midnight is attributed to the day its
        // registers were read instead of producing a full-day spike.
        val query = """
                WITH anchor AS (SELECT CAST(max(date_created) AS DATE) AS d
                                FROM raw_kpi_register_values)
                SELECT dt,
                       max - lag(max) OVER (ORDER BY dt) AS kwh
                FROM (SELECT CAST(r.date_created AS DATE) AS dt,
                             max($valueExpr)              AS max
                      FROM raw_kpi_register_values r, anchor
                      WHERE r.date_created >= anchor.d - 1
                        AND r.date_created < anchor.d + 1
                      GROUP BY CAST(r.date_created AS DATE)) tbl
                WHERE max IS NOT NULL
                ORDER BY dt DESC
                LIMIT 1;
                """.trimIndent()
        return transaction {
            // explicit type: Exposed misclassifies WITH-prefixed SQL as an update
            exec(query, explicitStatementType = StatementType.SELECT) { resultSet ->
                if (resultSet.next()) {
                    val kwh = resultSet.getDouble(kWhColumn)
                    if (resultSet.wasNull()) null else kwh
                } else {
                    null
                }
            }
        }
    }
}
