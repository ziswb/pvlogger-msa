package ch.bziswiler.pvlogger.masterdata.server.database.access

import ch.bziswiler.pvlogger.masterdata.server.database.schema.MasterDataTable
import ch.bziswiler.pvlogger.shared.masterdata.modbus.DeviceType
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Quantity
import ch.bziswiler.pvlogger.shared.masterdata.modbus.RegisterAddress
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Scope
import ch.bziswiler.pvlogger.shared.masterdata.modbus.dto.ModbusRegisterDefinition
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import ch.bziswiler.pvlogger.shared.database.dbTransaction

class MasterDataDao {

    private suspend fun <T> dbQuery(block: () -> T): T = dbTransaction { block() }

    suspend fun insertBatch(devType: DeviceType, registerDefinitions: List<ModbusRegisterDefinition>) = dbQuery {
        MasterDataTable.batchInsert(registerDefinitions, ignore = true, shouldReturnGeneratedValues = false) {
            this[MasterDataTable.deviceType] = devType
            this[MasterDataTable.no] = it.no
            this[MasterDataTable.name] = it.name
            this[MasterDataTable.readWrite] = it.readWrite
            this[MasterDataTable.type] = it.type
            this[MasterDataTable.unit] = it.unit
            this[MasterDataTable.gain] = it.gain
            this[MasterDataTable.address] = it.registerAddress.value
            this[MasterDataTable.quantity] = it.quantity.value
            this[MasterDataTable.scope] = it.scope.value
            this[MasterDataTable.category] = it.category
        }
    }

    suspend fun registerDefinitionList(whereCondition: Op<Boolean>) = dbQuery {
        MasterDataTable
            .selectAll()
            .where { whereCondition }
            .orderBy(MasterDataTable.address to SortOrder.ASC)
            .map { modbusRegisterDefinition(it) }
    }

    suspend fun registerDefinition(address: Int) = dbQuery {
        MasterDataTable
            .selectAll()
            .where(MasterDataTable.address eq address)
            .firstOrNull()
            ?.let { modbusRegisterDefinition(it) }
    }

    private fun modbusRegisterDefinition(row: ResultRow) = ModbusRegisterDefinition(
        row[MasterDataTable.no],
        row[MasterDataTable.name],
        row[MasterDataTable.readWrite],
        row[MasterDataTable.type],
        row[MasterDataTable.unit],
        row[MasterDataTable.gain],
        RegisterAddress(row[MasterDataTable.address]),
        Quantity(row[MasterDataTable.quantity]),
        Scope(row[MasterDataTable.scope].orEmpty()),
        row[MasterDataTable.category],
    )
}
