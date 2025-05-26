package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.response_time
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.rest.Measurement
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

object RegisterReader {

    private const val ONE = 1

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = response_time to DESC

    fun <T> readLatestRegisterValue(valueTable: IdTable<Int>, valueColumn: Column<T?>, registerAddressColumn: Column<Int>, registerAddress: RegisterAddress, deviceId: Int, jobId: JobId): Measurement<T>? = transaction {
        (valueTable innerJoin ResponseTable)
            .select(valueColumn, response_time)
            .where(
                registerAddressColumn.eq(registerAddress.value)
                .and(ResponseTable.logic_device_id.eq(deviceId.toUByte()))
                .and(ResponseTable.job_id.eq(jobId.id))
            )
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[valueColumn]?.let { Measurement<T>(row[response_time].toEpochSecond(UTC), it) } }
            .also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }
    }

    fun readKwSoFarToday(type: Type, registerAddress: RegisterAddress): Measurement<Int>? = when (type) {
        Type.I32 -> readKwSoFarToday(registerAddress)
        else -> throw NotImplementedError()
    }

    private fun readKwSoFarToday(registerAddress: RegisterAddress): Measurement<Int>? {
        val timestampColumn = 1
        val kWhColumn = 2
        return transaction {
            addLogger(StdOutSqlLogger)
            exec(
                """
                SELECT dt,
                       max - lag(max) OVER (ORDER BY dt) AS kwh
                FROM (SELECT mrd.date_created::date      AS dt,
                             max(mriv.register_value) AS max
                      FROM modbus_register_integer_values mriv
                               JOIN modbus_response_data mrd ON mriv.response_id = mrd.id
                      WHERE mriv.register_address = ${registerAddress.value}
                        AND (mrd.date_created::date = CURRENT_DATE
                          OR mrd.date_created::date = CURRENT_DATE - 1)
                      GROUP BY mrd.date_created::date) tbl
                ORDER BY dt DESC
                LIMIT 1;
                """.trimIndent()
            ) { resultSet ->
                resultSet.next()
                val timestamp = resultSet.getTimestamp(timestampColumn)
                val value = resultSet.getInt(kWhColumn)
                timestamp
                    ?.let { Measurement(it.toLocalDateTime().toEpochSecond(UTC), value) }
                    ?: Measurement(LocalDateTime.now().toEpochSecond(UTC), value)
            }
        }
    }
}