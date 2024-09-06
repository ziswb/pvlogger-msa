package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.stringify
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.delay

val delayMillis get() = 200L

suspend fun ByteReadChannel.readString(nOfBytes: Int): String {
    delay(delayMillis)
    return this.readPacket(nOfBytes).readBytes().stringify()
}

suspend fun ByteReadChannel.readMld(nOfShorts: Int): ShortArray {
    var result = shortArrayOf()
    delay(delayMillis)
    repeat(nOfShorts) { result = result.plus(this.readShort()) }
    return result
}

suspend fun ByteReadChannel.readUInt(): UInt {
    delay(delayMillis)
    return this.readInt().toUInt()
}

suspend fun ByteReadChannel.readUShort(): UShort {
    delay(delayMillis)
    return this.readShort().toUShort()
}

suspend fun ByteReadChannel.readUByte(): UByte {
    delay(delayMillis)
    return this.readByte().toUByte()
}
