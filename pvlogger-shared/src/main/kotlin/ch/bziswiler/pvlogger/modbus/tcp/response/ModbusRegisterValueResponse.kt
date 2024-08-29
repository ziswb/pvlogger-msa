package ch.bziswiler.pvlogger.modbus.tcp.response

import ch.bziswiler.pvlogger.messaging.meta.JobId
import ch.bziswiler.pvlogger.messaging.meta.RequestId
import ch.bziswiler.pvlogger.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime

data class ModbusRegisterValueResponse(
    override var jobId: JobId,
    override val requestId: RequestId,
    override var requestTime: LocalDateTime,
    override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var registerValues: List<ModbusRegisterValue>,
) : ModbusResponse {

    @Throws(IOException::class)
    @Suppress("unused")
    override fun writeObject(oos: ObjectOutputStream) {
        super.writeObject(oos)
        oos.writeObject(registerValues)
    }

    @Throws(IOException::class)
    @Suppress("unused", "UNCHECKED_CAST")
    override fun readObject(ois: ObjectInputStream) {
        super.readObject(ois)
        this.registerValues = ois.readObject() as List<ModbusRegisterValue>
    }
}
