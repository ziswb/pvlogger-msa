package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readShort

class MockDataReader(private val channel: ByteReadChannel) {

    var dataLength: UShort = 0u
    var protocolType: UShort = 0u
    var protocolIdentifier: UShort = 0u
    var logicDeviceId: UByte = 0u
    val values = mutableListOf<Any>()

    suspend fun readData(registerValues: List<RegisterValue<*>>) {
        protocolIdentifier = channel.readUShort()
        protocolType = channel.readUShort()
        dataLength = channel.readUShort()
        logicDeviceId = channel.readUByte()
        channel.readUByte() // function code
        channel.readUByte() // number of bytes
        registerValues.forEach {
            when (it.type) {
                Type.U16 -> values.add(channel.readUShort())
                Type.I16 -> values.add(channel.readShort())
                Type.I32 -> values.add(channel.readInt())
                else -> UnsupportedOperationException("Unknown type: $it")
            }
        }
    }

    override fun toString(): String {
        return """
protocolIdentifier: $protocolIdentifier
protocolType: $protocolType
dataLength: $dataLength
logicDeviceId: $logicDeviceId
values:
- ${values.joinToString("\n- ")}
""".trimIndent()
    }
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()

suspend fun ByteReadChannel.readUByte(): UByte = this.readByte().toUByte()
