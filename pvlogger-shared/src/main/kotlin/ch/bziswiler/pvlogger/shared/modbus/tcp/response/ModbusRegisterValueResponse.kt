package ch.bziswiler.pvlogger.shared.modbus.tcp.response

import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import ch.bziswiler.pvlogger.shared.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class ModbusRegisterValueResponse(
    override var jobId: JobId,
    override val requestId: RequestId,
    @Serializable(with = LocalDateTimeFromRfc3339Serializer::class) override var requestTime: LocalDateTime,
    @Serializable(with = LocalDateTimeFromRfc3339Serializer::class) override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var registerValues: List<ModbusRegisterValue<*>>,
) : ModbusResponse
