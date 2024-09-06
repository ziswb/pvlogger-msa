package ch.bziswiler.pvlogger.shared.modbus.tcp.frame

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

private const val TWO_BYTE = 0xFFFF

private const val ONE_BYTE = 0xFF

/*

    The kotlin.UByte is an unsigned 8-bit integer (0 – 255)
    The kotlin.UShort is an unsigned 16-bit integer (0 – 65535)
    The kotlin.UInt is an unsigned 32-bit integer (0 to 2^32 – 1)
    The kotlin.ULong is an unsigned 64-bit integer (0 to 2^64 -1)

 */
class MbapHeader(
    var protocolIdentifier: UShort,
    var protocolType: UShort = 0x0u,
    var dataLength: UShort,
    var logicDeviceId: UByte,
) : Serializable {

    @Throws(IOException::class)
    @Suppress("unused")
    fun writeObject(oos: ObjectOutputStream) {
        oos.writeInt(this.protocolIdentifier.toInt())
        oos.writeInt(this.protocolType.toInt())
        oos.writeInt(this.dataLength.toInt())
        oos.writeInt(this.logicDeviceId.toInt())
    }

    @Throws(IOException::class)
    @Suppress("unused")
    fun readObject(ois: ObjectInputStream) {
        this.protocolIdentifier = (ois.readInt() and TWO_BYTE).toUShort()
        this.protocolType = (ois.readInt() and TWO_BYTE).toUShort()
        this.dataLength = (ois.readInt() and TWO_BYTE).toUShort()
        this.logicDeviceId = (ois.readInt() and ONE_BYTE).toUByte()
    }

    companion object {

        fun readObject(ois: ObjectInputStream): MbapHeader {
            val protocolIdentifier: UShort = (ois.readInt() and TWO_BYTE).toUShort()
            val protocolType: UShort = (ois.readInt() and TWO_BYTE).toUShort()
            val dataLength: UShort = (ois.readInt() and TWO_BYTE).toUShort()
            val logicDeviceId: UByte = (ois.readInt() and ONE_BYTE).toUByte()
            return MbapHeader(protocolIdentifier, protocolType, dataLength, logicDeviceId)
        }
    }
}
