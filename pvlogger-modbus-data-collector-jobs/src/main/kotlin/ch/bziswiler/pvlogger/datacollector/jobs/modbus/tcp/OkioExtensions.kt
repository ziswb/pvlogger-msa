package ch.bziswiler.pvlogger.datacollector.jobs.modbus.tcp

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSink.writeUByte(value: UByte) {
    writeByte(value.toInt() and 0xFF)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSink.writeUShort(value: UShort) {
    // Okio writes big-endian by default
    writeShort(value.toInt() and 0xFFFF)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSink.writeUInt(value: UInt) {
    writeInt(value.toInt())
}

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSource.readUByte(): UByte = readByte().toUByte()

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSource.readUShort(): UShort = readShort().toUShort()

@OptIn(ExperimentalUnsignedTypes::class)
fun BufferedSource.readUInt(): UInt = readInt().toUInt()

fun ByteArray.toBuffer(): Buffer = Buffer().write(this)
