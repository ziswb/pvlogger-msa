package ch.bziswiler.pvlogger.dataaccessor.huawei

import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.logic_device_id
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable.request_time
import mu.KotlinLogging
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll

abstract class Device(private val deviceId: Int) {

    private val logger = KotlinLogging.logger {}
    private val default = 0.0

    abstract fun activePowerKW(): Double

    protected fun activePowerKW(registerAddress: Int, gain: Double): Double = (IntegerValueTable innerJoin ResponseTable)
        .selectAll()
        .where { register_address.eq(registerAddress).and(logic_device_id.eq(deviceId.toUByte())) }
        .orderBy(request_time to DESC)
        .firstOrNull()
        .let { row -> row?.get(IntegerValueTable.register_value) }
        .let { value -> value?.toDouble()?.div(gain) ?: default.also { logger.info { "'device value' is null; returning default value '$default'" } } }
}
