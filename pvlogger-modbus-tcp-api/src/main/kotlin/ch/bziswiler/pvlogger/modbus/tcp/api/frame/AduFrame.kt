package ch.bziswiler.pvlogger.modbus.tcp.api.frame

import ch.bziswiler.pvlogger.modbus.base.tcp.encode
import ch.bziswiler.pvlogger.modbus.tcp.api.frame.*
import kotlinx.io.Sink

class AduFrame(private val header: MbapHeader, private val functionCode: FunctionCode, private val data: Data<*>) {
    suspend fun encode(channel: Sink) {
        header.encode(channel)
        functionCode.encode(channel)
        if (data is UShortData) {
            data.encode(channel)
        } else if (data is UByteData) {
            data.encode(channel)
        }
    }
}
