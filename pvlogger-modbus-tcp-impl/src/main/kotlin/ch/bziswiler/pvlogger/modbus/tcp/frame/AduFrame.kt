package ch.bziswiler.pvlogger.modbus.tcp.frame

import ch.bziswiler.pvlogger.modbus.api.tcp.FunctionCode
import ch.bziswiler.pvlogger.modbus.api.tcp.MbapHeader
import ch.bziswiler.pvlogger.modbus.tcp.encode
import kotlinx.serialization.Serializable
import okio.BufferedSink

@Serializable
class AduFrame(
    private val header: MbapHeader,
    private val functionCode: FunctionCode,
    private val data: Data<*>,
) {
    fun encode(sink: BufferedSink) {
        header.encode(sink)
        functionCode.encode(sink)
        if (data is UShortData) {
            data.encode(sink)
        } else if (data is UByteData) {
            data.encode(sink)
        }
    }
}
