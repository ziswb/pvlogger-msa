package ch.bziswiler.pvlogger.modbus.tcp

import ch.bziswiler.pvlogger.modbus.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.api.masterdata.dto.ModbusRegisterDefinition

data class ModbusRegisterStringValue(
    override var definition: ModbusRegisterDefinition,
    override var value: String,
) : ModbusRegisterValue<String>
