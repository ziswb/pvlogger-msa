package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type

interface RegisterValue<T> {
    val type: Type
    val value: T
    val quantity: Int
        get() {
            return when (type) {
                Type.U16, Type.I16, Type.BITFIELD_16 -> 2
                Type.U32, Type.I32, Type.BITFIELD_32 -> 4
                else -> throw NotImplementedError()
            }
        }
}