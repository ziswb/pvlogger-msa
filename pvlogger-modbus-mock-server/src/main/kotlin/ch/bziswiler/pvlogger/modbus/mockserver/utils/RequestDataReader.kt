package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import io.ktor.utils.io.ByteReadChannel
import mu.KotlinLogging

class RequestDataReader(private val channel: ByteReadChannel) {

    private val logger = KotlinLogging.logger {}

    suspend fun readRequest(): Pair<RegisterAddress, Int> {
        val protocolIdentifier = channel.readUShort()
        logger.trace { "protocol identifier: $protocolIdentifier" }
        val protocolType = channel.readUShort()
        logger.trace { "protocol type: $protocolType" }
        val dataLength = channel.readUShort()
        logger.trace { "data length: $dataLength" }
        val logicDeviceId = channel.readUByte()
        logger.trace { "logic deviceId: $logicDeviceId" }
        val functionCode = channel.readUByte()
        logger.trace { "function code: $functionCode" }
        val registerAddress = channel.readUShort()
        logger.trace { "register address: $registerAddress" }
        val numberOfBytes = channel.readUShort()
        logger.trace { "number of bytes: $numberOfBytes" }
        var overflow = 0u
        while (channel.availableForRead > 0) {
            overflow += channel.readUByte()
        }
        logger.trace {
            """Values:
        protocolIdentifier = '$protocolIdentifier'
        protocolType = '$protocolType'
        dataLength = '$dataLength'
        logicDeviceId = '$logicDeviceId'
        functionCode = '$functionCode'
        registerAddress = '$registerAddress'
        numberOfBytes = '$numberOfBytes'
        overflow = '$overflow'
        """.trimIndent()
        }
        return RegisterAddress(registerAddress.toInt()) to numberOfBytes.toInt()
    }
}

suspend fun ByteReadChannel.readUShort(): UShort = this.readShort().toUShort()

suspend fun ByteReadChannel.readUByte(): UByte = this.readByte().toUByte()
