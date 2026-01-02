package ch.bziswiler.pvlogger.shared.masterdata.modbus.dto

import ch.bziswiler.pvlogger.shared.masterdata.modbus.Category
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Gain
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Quantity
import ch.bziswiler.pvlogger.shared.masterdata.modbus.ReadWrite
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Scope
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Unit
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ModbusRegisterDefinition(
    val no: Int,
    val name: String,
    val readWrite: ReadWrite,
    val type: Type,
    val unit: Unit,
    val gain: Gain,
    @Contextual
    val registerAddress: RegisterAddress,
    @Contextual
    val quantity: Quantity,
    @Contextual
    val scope: Scope,
    val category: Category,
)
