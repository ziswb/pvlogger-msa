package ch.bziswiler.pvlogger.datacollector.services.modbus.request

import ch.bziswiler.pvlogger.datacollector.services.modbus.readUByte
import ch.bziswiler.pvlogger.datacollector.services.modbus.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode.Companion.from
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.FunctionCode.ReadRegisters
import ch.bziswiler.pvlogger.shared.job.meta.JobId
import ch.bziswiler.pvlogger.shared.job.meta.RequestId
import kotlinx.io.Source
import mu.KotlinLogging
import java.time.LocalDateTime.now

private const val LENGTH_OF_DEVICE_ID_FUNCTION_CODE_AND_NUMBER_OF_BYTES: UByte = 3u

abstract class ModbusRegisterValueRequest : ModbusRequest {

    private val logger = KotlinLogging.logger {}

    override suspend fun decode(channel: Source): ModbusRegisterValueResponse =
        run {
            logger.debug("ModbusRegisterValueRequest.decode()")
            val header = readHeader(channel)
            val functionCode = from(channel.readUByte())
            ModbusRegisterValueResponse(
                jobId = JobId(jobId),
                requestId = RequestId(requestId),
                requestTime = requestTime,
                responseTime = now(),
                header = header,
                functionCode = functionCode,
                numberOfBytes = header.dataLength.minus(LENGTH_OF_DEVICE_ID_FUNCTION_CODE_AND_NUMBER_OF_BYTES)
                    .toUByte(),
                registerValues = readData(functionCode, channel),
            )
        }

    private suspend fun readData(
        functionCode: FunctionCode,
        channel: Source,
    ): List<ModbusRegisterValue<*>> {
        logger.debug("ModbusRegisterValueRequest.readData()")
        if (functionCode == ReadRegisters) {
            readNumberOfBytes(channel)
            return readRegisters(channel)
        } else {
            throw UnsupportedOperationException("Unknown function code '${functionCode.value}'")
        }
    }

    protected abstract suspend fun readRegisters(channel: Source): List<ModbusRegisterValue<*>>

    private fun readNumberOfBytes(channel: Source) = channel.readUByte()
}
