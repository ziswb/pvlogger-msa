package ch.bziswiler.pvlogger.dataaccessor.schema

import kotlin.experimental.and

@OptIn(ExperimentalUnsignedTypes::class)
fun UByteArray.toInt() = this.fold(0) { i: Int, ub: UByte -> (i shl 8) + (ub.toByte() and 0xf) }
