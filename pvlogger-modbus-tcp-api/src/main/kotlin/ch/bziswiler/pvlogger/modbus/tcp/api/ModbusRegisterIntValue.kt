package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

data class ModbusRegisterIntValue(
    override var definition: ModbusRegisterDefinition,
    override var value: Int
) : ModbusRegisterValue<Int>
