package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.RawKpiRegisterValuesTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.insert
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
            it[meterExportedEnergy] = values.meterExportedEnergy
            it[meterImportedEnergy] = values.meterImportedEnergy
        }
    }

    fun meterEnergyToday(): MeterEnergyToday = transaction {
        logger.info { "computing meter energy today" }
        val exported = computeEnergyDelta("meter_exported_energy")
        val imported = computeEnergyDelta("meter_imported_energy")
        MeterEnergyToday(
            exportedEnergyToday = exported ?: 0.0,
            importedEnergyToday = imported ?: 0.0,
        )
    }

    private fun computeEnergyDelta(columnName: String): Double? {
        val kWhColumn = 2
        return transaction {
            exec(
                """
                SELECT dt,
                       max - lag(max) OVER (ORDER BY dt) AS kwh
                FROM (SELECT r.date_created::date AS dt,
                             max(r.$columnName)   AS max
                      FROM raw_kpi_register_values r
                      WHERE r.date_created >= CURRENT_DATE - 1
                        AND r.date_created < CURRENT_DATE + 1
                      GROUP BY r.date_created::date) tbl
                WHERE max IS NOT NULL
                ORDER BY dt DESC
                LIMIT 1;
                """.trimIndent(),
            ) { resultSet ->
                if (resultSet.next()) {
                    resultSet.getDouble(kWhColumn)
                } else {
                    null
                }
            }
        }
    }
}
