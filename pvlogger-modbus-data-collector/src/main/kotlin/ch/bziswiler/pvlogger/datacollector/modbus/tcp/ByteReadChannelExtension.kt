package ch.bziswiler.pvlogger.datacollector.modbus.tcp

import kotlinx.coroutines.delay
import okio.BufferedSource

val delayMillis get() = 200L

suspend fun BufferedSource.readString(nOfBytes: Int): String {
    delay(delayMillis)
    return this.readByteArray(nOfBytes.toLong()).toString()
}

suspend fun BufferedSource.readMld(nOfShorts: Int): ShortArray {
    var result = shortArrayOf()
    delay(delayMillis)
    repeat(nOfShorts) { result = result.plus(this.readShort()) }
    return result
}

