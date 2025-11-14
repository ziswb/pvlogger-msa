package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

@Serializable
sealed interface ModbusRegisterValue<T> {
    val definition: ModbusRegisterDefinition
    val value: T
}
