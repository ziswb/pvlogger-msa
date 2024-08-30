package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.masterdata.modbus.Type

data class RegisterShortValue(override val type: Type = Type.I16, override val value: Short) : RegisterValue<Short>