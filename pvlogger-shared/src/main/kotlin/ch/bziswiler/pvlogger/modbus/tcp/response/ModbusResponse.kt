package ch.bziswiler.pvlogger.modbus.tcp.response

import ch.bziswiler.pvlogger.messaging.meta.JobId
import ch.bziswiler.pvlogger.messaging.meta.RequestId
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.LocalDateTime

private const val ONE_BYTE = 0xFF

interface ModbusResponse : Serializable {
    var jobId: JobId
    val requestId: RequestId
    var requestTime: LocalDateTime
    var responseTime: LocalDateTime
    var header: MbapHeader
    var functionCode: FunctionCode
    var numberOfBytes: UByte

    @Throws(IOException::class)
    @Suppress("unused")
    fun writeObject(oos: ObjectOutputStream) {
        oos.writeObject(jobId)
        oos.writeObject(requestTime)
        oos.writeObject(responseTime)
        this.header.writeObject(oos)
        oos.writeObject(functionCode)
        oos.writeInt(numberOfBytes.toInt())
    }

    @Throws(IOException::class)
    @Suppress("unused")
    fun readObject(ois: ObjectInputStream) {
        this.jobId = ois.readObject() as JobId
        this.requestTime = ois.readObject() as LocalDateTime
        this.responseTime = ois.readObject() as LocalDateTime
        this.header = MbapHeader.readObject(ois)
        this.functionCode = ois.readObject() as FunctionCode
        this.numberOfBytes = (ois.readInt() and ONE_BYTE).toUByte()
    }
}
