package ch.bziswiler.pvlogger.modbus.tcp.api

import ch.bziswiler.pvlogger.modbus.tcp.api.masterdata.ModbusRegisterDefinition

interface ModbusRegisterValue<T> {
    var definition: ModbusRegisterDefinition
    var value: T
}
