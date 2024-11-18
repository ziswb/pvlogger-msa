package ch.bziswiler.pvlogger.dataaccessor.huawei

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_value
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.response_time
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

abstract class Device(private val deviceId: Int) {

    private val logger = KotlinLogging.logger {}
    private val responseTimeDescending = response_time to DESC
    private val default = 0.0

    abstract fun activePowerKW(): Double

    protected fun readAndTransposeLatestRegisterValue(registerAddress: Int, gain: Double): Double {
        return (IntegerValueTable innerJoin ResponseTable)
            .selectAll()
            .where { register_address.eq(registerAddress).and(logic_device_id.eq(deviceId.toUByte())) }
            .orderBy(responseTimeDescending)
            .firstOrNull()
            .let { row ->
                row?.get(register_value)
                    ?.toDouble()
                    ?.div(gain)
                    ?: default.also { logger.info { "'Register value' is null. Returning default value: '$default'." } }
            }
    }
}
