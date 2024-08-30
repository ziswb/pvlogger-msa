package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.masterdata.modbus.Type

data class RegisterUShortValue(override val type: Type = Type.U16, override val value: UShort) : RegisterValue<UShort>