package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

@Serializable(with = ModbusRegisterValueSerializer::class)
sealed interface ModbusRegisterValue<T> {
    val definition: ModbusRegisterDefinition
    val value: T
}
