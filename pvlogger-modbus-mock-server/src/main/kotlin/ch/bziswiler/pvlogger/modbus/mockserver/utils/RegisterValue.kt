package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type

interface RegisterValue<T> {
    val type: Type
    val value: T
}