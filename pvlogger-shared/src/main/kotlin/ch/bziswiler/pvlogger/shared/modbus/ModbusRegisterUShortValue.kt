package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

private const val TWO_BYTES = 0xFFFF

@Serializable
data class ModbusRegisterUShortValue(override var definition: ModbusRegisterDefinition, override var value: UShort) :
    ModbusRegisterValue<UShort>
