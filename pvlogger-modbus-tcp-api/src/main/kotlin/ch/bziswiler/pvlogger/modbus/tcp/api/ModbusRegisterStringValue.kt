package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

data class ModbusRegisterStringValue(
    override var definition: ModbusRegisterDefinition,
    override var value: String
) : ModbusRegisterValue<String>
