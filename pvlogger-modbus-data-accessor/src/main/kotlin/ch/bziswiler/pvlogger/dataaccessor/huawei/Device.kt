package ch.bziswiler.pvlogger.business.model.huawei.device

import ch.bziswiler.pvlogger.dataaccessor.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.IntegerValueTable.register_address
import ch.bziswiler.pvlogger.dataaccessor.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.schema.ResponseTable.logic_device_id
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
        .orderBy(ResponseTable.request_time to DESC)
        .firstOrNull()
        .let { row -> row?.get(IntegerValueTable.register_value) }
        .let { value -> projectedValue(value, gain) }

    private fun projectedValue(value: Int?, scalar: Double): Double =
        if (value != null) {
            value.toDouble().div(scalar)
        } else {
            logger.info { "'device value' is null; returning default value '$default'" }
            default
        }
}