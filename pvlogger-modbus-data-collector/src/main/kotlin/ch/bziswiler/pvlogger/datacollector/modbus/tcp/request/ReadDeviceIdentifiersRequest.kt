package ch.bziswiler.pvlogger.datacollector.modbus.tcp.request

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.decodeModbusObject
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.frame.AduFrame
import ch.bziswiler.pvlogger.datacollector.modbus.tcp.readUByte
import ch.bziswiler.pvlogger.modbus.ModbusObject
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.Companion.from
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode.ReadDeviceIdentifiers
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ReadDeviceIdentifiersResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import java.time.LocalDateTime
import java.time.LocalDateTime.now

open class ReadDeviceIdentifiersRequest(
    final override val jobId: String,
    final override val requestTime: LocalDateTime,
    final override val requestId: UShort,
    final override val logicDeviceId: UByte,
    final override val dataLength: UShort = 0x05u,
    final override val functionCode: FunctionCode = ReadDeviceIdentifiers,
    meiType: UByte = 0x0Eu,
    readDevIdCode: UByte = 0x01u,
    objectId: UByte = 0x00u,
) : ModbusRequest {
    private val frame =
        AduFrame(
            MbapHeader(requestId, dataLength = dataLength, logicDeviceId = logicDeviceId),
            functionCode,
            UByteData(listOf(meiType, readDevIdCode, objectId)),
        )

    override suspend fun encode(channel: ByteWriteChannel) = frame.encode(channel)

    override suspend fun decode(channel: ByteReadChannel): ModbusResponse {
        val header = readHeader(channel)
        val functionCode = from(channel.readUByte())
        val meiType = channel.readUByte()
        val readDevId = channel.readUByte()
        val consistencyLevel = channel.readUByte()
        val more = channel.readUByte()
        val nextObjectId = channel.readUByte()
        val numberOfObjects = channel.readUByte()
        val objectList = mutableListOf<ModbusObject>()
        repeat(numberOfObjects.toInt()) { objectList.add(decodeModbusObject(channel)) }
        return ReadDeviceIdentifiersResponse(
            jobId,
            requestTime,
            now(),
            header,
            functionCode,
            numberOfBytes = 0x0u,
            meiType,
            readDevId,
            consistencyLevel,
            more,
            nextObjectId,
            numberOfObjects,
            objectList,
        )
    }
}
