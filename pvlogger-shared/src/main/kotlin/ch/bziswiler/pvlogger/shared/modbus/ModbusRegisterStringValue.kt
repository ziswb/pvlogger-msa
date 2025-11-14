package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

@Serializable
data class ModbusRegisterStringValue(override var definition: ModbusRegisterDefinition, override var value: String) :
    ModbusRegisterValue<String>
