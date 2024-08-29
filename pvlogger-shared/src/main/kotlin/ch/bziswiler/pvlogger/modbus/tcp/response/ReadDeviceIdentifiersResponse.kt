package ch.bziswiler.pvlogger.modbus.tcp.response

import ch.bziswiler.pvlogger.modbus.ModbusObject
import ch.bziswiler.pvlogger.modbus.tcp.frame.FunctionCode
import ch.bziswiler.pvlogger.modbus.tcp.frame.MbapHeader
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime

private const val ONE_BYTE = 0xFF

data class ReadDeviceIdentifiersResponse(
    override var jobId: String,
    override val requestId: UShort,
    override var requestTime: LocalDateTime,
    override var responseTime: LocalDateTime,
    override var header: MbapHeader,
    override var functionCode: FunctionCode,
    override var numberOfBytes: UByte,
    var meiType: UByte = 0x0Eu,
    var readDevIdCode: UByte = 0x01u,
    var consistencyLevel: UByte = 0x01u,
    var more: UByte,
    var nextObjectId: UByte,
    var numberOfObjects: UByte,
    var objectList: List<ModbusObject>,
) : ModbusResponse {

    @Throws(IOException::class)
    @Suppress("unused")
    override fun writeObject(oos: ObjectOutputStream) {
        super.writeObject(oos)
        oos.writeInt(meiType.toInt())
        oos.writeInt(readDevIdCode.toInt())
        oos.writeInt(consistencyLevel.toInt())
        oos.writeInt(more.toInt())
        oos.writeInt(nextObjectId.toInt())
        oos.writeInt(numberOfObjects.toInt())
        oos.writeObject(objectList)
    }

    @Throws(IOException::class)
    @Suppress("unused", "UNCHECKED_CAST")
    override fun readObject(ois: ObjectInputStream) {
        super.readObject(ois)
        this.meiType = (ois.readInt() and ONE_BYTE).toUByte()
        this.readDevIdCode = (ois.readInt() and ONE_BYTE).toUByte()
        this.consistencyLevel = (ois.readInt() and ONE_BYTE).toUByte()
        this.more = (ois.readInt() and ONE_BYTE).toUByte()
        this.nextObjectId = (ois.readInt() and ONE_BYTE).toUByte()
        this.numberOfObjects = (ois.readInt() and ONE_BYTE).toUByte()
        this.objectList = ois.readObject() as List<ModbusObject>
    }

    override fun toString(): String =
        """
        MEI Type: $meiType
        Read Device ID: $readDevIdCode
        Conformity Level: $consistencyLevel
        More Follows: $more
        Next Object ID: $nextObjectId
        Number of Objects: $numberOfObjects
        Objects: ${objectList.joinToString()}
        """
}
