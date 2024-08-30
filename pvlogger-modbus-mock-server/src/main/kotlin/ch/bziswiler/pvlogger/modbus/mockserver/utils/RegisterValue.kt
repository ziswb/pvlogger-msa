package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.masterdata.modbus.Type

interface RegisterValue<T> {
    val type: Type
    val value: T
}