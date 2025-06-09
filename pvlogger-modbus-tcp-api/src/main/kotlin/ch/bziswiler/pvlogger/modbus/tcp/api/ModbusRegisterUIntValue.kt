package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

data class ModbusRegisterUIntValue(
    override var definition: ModbusRegisterDefinition,
    override var value: UInt
) : ModbusRegisterValue<UInt>
