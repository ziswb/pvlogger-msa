package ch.bziswiler.pvlogger.modbus.mockserver.utils

import io.ktor.utils.io.ByteReadChannel

class RequestDataReader(private val channel: ByteReadChannel) {

    suspend fun readRequest(): String {
        val protocolIdentifier = channel.readUShort()
        val protocolType = channel.readUShort()
        val dataLength = channel.readUShort()
        val logicDeviceId = channel.readUByte()
        val functionCode = channel.readUByte()
        val registerAddresses = mutableListOf<UByte>()
        while (channel.availableForRead > 0) {
            registerAddresses.add(channel.readUByte())
        }
        return """Values:
        protocolIdentifier = '$protocolIdentifier'
        protocolType = '$protocolType'
        dataLength = '$dataLength'
        logicDeviceId = '$logicDeviceId'
        functionCode = '$functionCode'
        registerAddresses = 
        - ${registerAddresses.joinToString("\n- ")}
        """.trimIndent()
    }
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()

suspend fun ByteReadChannel.readUByte(): UByte = this.readByte().toUByte()
