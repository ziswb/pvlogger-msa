package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

data class ModbusRegisterUShortValue(
    override var definition: ModbusRegisterDefinition,
    override var value: UShort
) : ModbusRegisterValue<UShort>
