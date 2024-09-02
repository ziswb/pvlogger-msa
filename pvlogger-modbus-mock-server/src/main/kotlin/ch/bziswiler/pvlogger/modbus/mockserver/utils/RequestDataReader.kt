package ch.bziswiler.pvlogger.modbus.mockserver.utils

import io.ktor.utils.io.ByteReadChannel

class RequestDataReader(private val channel: ByteReadChannel) {

    suspend fun readRequest(): String {
        val protocolIdentifier = channel.readUShort()
        val protocolType = channel.readUShort()
        val dataLength = channel.readUShort()
        val logicDeviceId = channel.readUByte()
        val functionCode = channel.readUByte()
        val registerAddress = channel.readUShort()
        val numberOfRegisters = channel.readUShort()
        var overflow = 0u
        while (channel.availableForRead > 0) {
            overflow += channel.readUByte()
        }
        return """Values:
        protocolIdentifier = '$protocolIdentifier'
        protocolType = '$protocolType'
        dataLength = '$dataLength'
        logicDeviceId = '$logicDeviceId'
        functionCode = '$functionCode'
        registerAddress = '$registerAddress'
        numberOfRegisters = '$numberOfRegisters'
        overflow = '$overflow'
        """.trimIndent()
    }
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()

suspend fun ByteReadChannel.readUByte(): UByte = this.readByte().toUByte()
