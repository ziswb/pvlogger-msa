package ch.bziswiler.pvlogger.shared.modbus

import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

@Serializable
data class ModbusRegisterUIntValue(override var definition: ModbusRegisterDefinition, override var value: UInt) :
    ModbusRegisterValue<UInt>
