package ch.bziswiler.pvlogger.modbus.mockserver.utils

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type

data class RegisterUIntValue(override val type: Type = Type.U32, override val value: UInt) : RegisterValue<UInt>