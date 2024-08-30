package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.masterdata.modbus.Type

data class RegisterIntValue(override val type: Type = Type.I32, override val value: Int) : RegisterValue<Int>