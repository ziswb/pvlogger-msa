package ch.bziswiler.pvlogger.modbus.tcp.response

import ch.bziswiler.pvlogger.modbus.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.api.meta.JobId
import ch.bziswiler.pvlogger.modbus.api.meta.RequestId
import ch.bziswiler.pvlogger.modbus.api.tcp.FunctionCode
import ch.bziswiler.pvlogger.modbus.api.tcp.MbapHeader
import ch.bziswiler.pvlogger.modbus.api.tcp.ModbusResponse
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class ModbusRegisterValueResponse(
    override var jobId: JobId,
    override val requestId: RequestId,
    override var requestTime: LocalDateTime,
    override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var registerValues: List<ModbusRegisterValue<*>>,
) : ModbusResponse
