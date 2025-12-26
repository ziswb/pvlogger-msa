package ch.bziswiler.pvlogger.datacollector.modbus.tcp.frame

import ch.bziswiler.pvlogger.datacollector.modbus.tcp.encode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.Data
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.MbapHeader
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UByteData
import ch.bziswiler.pvlogger.shared.modbus.tcp.frame.UShortData
import okio.BufferedSink

class AduFrame(private val header: MbapHeader, private val functionCode: FunctionCode, private val data: Data<*>) {
    suspend fun encode(sink: BufferedSink) {
        header.encode(sink)
        functionCode.encode(sink)
        if (data is UShortData) {
            data.encode(sink)
        } else if (data is UByteData) {
            data.encode(sink)
        }
    }
}
