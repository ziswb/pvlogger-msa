package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

data class ModbusRegisterShortValue(
    override var definition: ModbusRegisterDefinition,
    override var value: Short
) : ModbusRegisterValue<Short>
