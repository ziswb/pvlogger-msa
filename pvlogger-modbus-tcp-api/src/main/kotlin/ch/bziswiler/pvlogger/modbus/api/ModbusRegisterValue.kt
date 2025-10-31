package ch.bziswiler.pvlogger.modbus.api

import ch.bziswiler.pvlogger.modbus.api.masterdata.dto.ModbusRegisterDefinition
import kotlinx.serialization.Serializable

@Serializable
interface ModbusRegisterValue<T> {

    var definition: ModbusRegisterDefinition
    var value: T
}
