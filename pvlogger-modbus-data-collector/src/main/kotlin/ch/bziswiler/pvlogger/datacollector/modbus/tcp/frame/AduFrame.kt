package ch.bziswiler.pvlogger.datacollector.modbus.tcp.frame

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.encode
import ch.bziswiler.pvlogger.modbus.tcp.frame.Data
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.modbus.tcp.frame.UShortData
import io.ktor.utils.io.ByteWriteChannel

class AduFrame(private val header: MbapHeader, private val functionCode: FunctionCode, private val data: Data<*>) {
    suspend fun encode(channel: ByteWriteChannel) {
        header.encode(channel)
        functionCode.encode(channel)
        if (data is UShortData) {
            data.encode(channel)
        } else if (data is UByteData) {
            data.encode(channel)
        }
    }
}
