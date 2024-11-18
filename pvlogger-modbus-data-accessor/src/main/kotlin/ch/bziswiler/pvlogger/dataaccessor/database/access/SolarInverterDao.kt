package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_value
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.request_time
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

object SolarInverterDao {

    private val logger = KotlinLogging.logger {}

    private const val ACTIVE_POWER_REGISTER_ADDRESS = 32080 // -> kW
    private const val DAILY_ENERGY_YIELD_REGISTER_ADDRESS = 32114 // -> kWh
    private const val ZERO = 0.0

    fun latestActivePowerKW(deviceId: Int, gain: Gain): Double = (IntegerValueTable innerJoin ResponseTable)
        .select(register_value)
        .where(register_address.eq(ACTIVE_POWER_REGISTER_ADDRESS).and(logic_device_id.eq(deviceId.toUByte())))
        .orderBy(request_time to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.let { row -> row[register_value]?.div(gain.raw.toDouble()) }
        ?: ZERO.also { logger.info { "'active power value' is null; returning default value '$ZERO'" } }

    fun latestDailyEnergyYieldKWh(deviceId: Int, gain: Gain): Double = (UIntegerValueTable innerJoin ResponseTable)
        .select(UIntegerValueTable.register_value)
        .where(register_address.eq(DAILY_ENERGY_YIELD_REGISTER_ADDRESS).and(logic_device_id.eq(deviceId.toUByte())))
        .orderBy(request_time to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.let { row -> row[UIntegerValueTable.register_value]?.toDouble()?.div(gain.raw.toDouble()) }
        ?: ZERO.also { logger.info { "'energy yield value' is null; returning default value '$ZERO'" } }
}