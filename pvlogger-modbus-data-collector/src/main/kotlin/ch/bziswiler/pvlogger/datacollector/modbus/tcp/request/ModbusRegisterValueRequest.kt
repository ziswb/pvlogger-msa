package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.Companion.from
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.ReadRegisters
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusRegisterValueResponse
import io.ktor.utils.io.ByteReadChannel
import mu.KotlinLogging
import java.time.LocalDateTime.now

abstract class ModbusRegisterValueRequest : ModbusRequest {

    private val logger = KotlinLogging.logger {}

    override suspend fun decode(channel: ByteReadChannel) =
        run {
            logger.debug("ModbusRegisterValueRequest.decode()")
            val header = readHeader(channel)
            val functionCode = from(channel.readUByte())
            ModbusRegisterValueResponse(
                jobId = jobId,
                requestTime = requestTime,
                responseTime = now(),
                header = header,
                functionCode = functionCode,
                numberOfBytes = header.dataLength.toInt().minus(3).toUByte(),
                registerValues = readData(functionCode, channel),
            )
        }

    private suspend fun readData(
        functionCode: FunctionCode,
        channel: ByteReadChannel,
    ): List<ModbusRegisterValue> {
        logger.debug("ModbusRegisterValueRequest.readData()")
        if (functionCode == ReadRegisters) {
            readNumberOfBytes(channel)
            return readRegisters(channel)
        } else {
            throw UnsupportedOperationException("Unknown function code '${functionCode.value}'")
        }
    }

    protected abstract suspend fun readRegisters(channel: ByteReadChannel): List<ModbusRegisterValue>

    private suspend fun readNumberOfBytes(channel: ByteReadChannel) = channel.readUByte()
}
