package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UShortValueTable
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

object RegisterReader {

    private const val ONE = 1

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = ResponseTable.response_time to DESC

    fun readLatestRegisterValue(type: Type, deviceId: Int, registerAddress: RegisterAddress): Any? = when (type) {
        Type.U16, Type.BITFIELD_16 -> readLatestRegisterValue(
            UShortValueTable,
            UShortValueTable.register_value,
            UShortValueTable.register_address,
            registerAddress,
            deviceId
        )?.toUShort()

        Type.U32, Type.BITFIELD_32 -> readLatestRegisterValue(
            UIntegerValueTable,
            UIntegerValueTable.register_value,
            UIntegerValueTable.register_address,
            registerAddress,
            deviceId
        )?.toUInt()

        Type.I16 -> readLatestRegisterValue(
            ShortValueTable,
            ShortValueTable.register_value,
            ShortValueTable.register_address,
            registerAddress,
            deviceId
        )?.toShort()

        Type.I32 -> readLatestRegisterValue(
            IntegerValueTable,
            IntegerValueTable.register_value,
            IntegerValueTable.register_address,
            registerAddress,
            deviceId
        )?.toInt()

        Type.STR -> readLatestRegisterValue(
            StringValuesTable,
            StringValuesTable.register_value,
            StringValuesTable.register_address,
            registerAddress,
            deviceId
        )?.toString()

        Type.MLD -> throw NotImplementedError()
    }

    private fun <T> readLatestRegisterValue(valueTable: IdTable<Int>, valueColumn: Column<T?>, registerAddressColumn: Column<Int>, registerAddress: RegisterAddress, deviceId: Int): T? = transaction {
        (valueTable innerJoin ResponseTable)
            .select(valueColumn)
            .where(registerAddressColumn.eq(registerAddress.value).and(ResponseTable.logic_device_id.eq(deviceId.toUByte())))
            .orderBy(responseTimeDescending)
            .limit(ONE)
            .firstOrNull()
            ?.let { row -> row[valueColumn] }
            ?: null.also { logger.info { "No register value found for device ID '$deviceId' and register address '$registerAddress'." } }
    }

    fun readSoFarTodayRegisterValue(type: Type, registerAddress: RegisterAddress): Int = when (type) {
        Type.I32 -> readSoFarTodayRegisterValue(registerAddress)
        else -> throw NotImplementedError()
    }

    private fun readSoFarTodayRegisterValue(registerAddress: RegisterAddress): Int = transaction {
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
            resultSet.getInt("kwh")
        }.let { it ?: 0 }
    }
}