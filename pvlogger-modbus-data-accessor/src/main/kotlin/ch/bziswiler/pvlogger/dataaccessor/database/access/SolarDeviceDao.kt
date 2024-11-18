package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_value
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.response_time
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.unit.KW
import ch.bziswiler.pvlogger.shared.masterdata.unit.KWh
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and

abstract class SolarDeviceDao(private val deviceId: Int, val activePowerRegisterAddress: RegisterAddress) {

    protected abstract val activePowerGain: Gain

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = response_time to DESC
    private val zero = 0.0

    open fun activePowerKW(): KW = readAndTransposeLatestRegisterValue(activePowerRegisterAddress, activePowerGain).toKW()

    protected fun readAndTransposeLatestRegisterValue(registerAddress: RegisterAddress, gain: Gain): Double = (IntegerValueTable innerJoin ResponseTable)
        .select(register_value)
        .where(register_address.eq(registerAddress.value).and(logic_device_id.eq(deviceId.toUByte())))
        .orderBy(responseTimeDescending)
        .limit(1)
        .firstOrNull()
        ?.let { row -> row[register_value]?.div(gain.raw.toDouble()) }
        ?: zero.also { logger.info { "'Register value' is null. Returning default value: '$zero'." } }

}

internal fun Double.toKWh(): KWh = KWh(this)
internal fun Double.toKW(): KW = KW(this)
