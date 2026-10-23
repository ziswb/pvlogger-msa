package ch.bziswiler.testutils

import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.RawKpiRegisterValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

object TestDataFactory {

    fun cleanAllTables() {
        transaction {
            IntegerValueTable.deleteAll()
            ResponseTable.deleteAll()
            HousePowerKpiTable.deleteAll()
            RawKpiRegisterValuesTable.deleteAll()
        }
    }

    fun seedResponseRow(
        id: Int,
        jobId: String,
        logicDeviceId: UByte = 1u,
        responseTime: Instant = Instant.now(),
    ) {
        transaction {
            ResponseTable.insert {
                it[ResponseTable.id] = id
                it[timestamp] = responseTime
                it[response_time] = responseTime
                it[request_time] = responseTime
                it[job_id] = jobId
                it[request_id] = 1u
                it[logic_device_id] = logicDeviceId
                it[function_code] = 1u
                it[data_length] = 1u
                it[protocol_type] = 1u
            }
        }
    }

    fun seedIntegerValue(responseId: Int, registerAddress: Int, value: Int) {
        transaction {
            IntegerValueTable.insert {
                it[response_id] = responseId
                it[register_address] = registerAddress
                it[register_value] = value
            }
        }
    }

    fun seedUIntegerValue(responseId: Int, registerAddress: Int, value: ULong) {
        transaction {
            UIntegerValueTable.insert {
                it[response_id] = responseId
                it[register_address] = registerAddress
                it[register_value] = value
            }
        }
    }

    fun seedKpiRow(
        dateCreated: Instant,
        solarPower: Double = 0.0,
        solarToHousePower: Double = 0.0,
        gridToHousePower: Double = 0.0,
        solarToGridPower: Double = 0.0,
        ownConsumption: Double = 0.0,
        dailyEnergyYield: Double = 0.0,
        dailyEnergySelfCovered: Double = 0.0,
        dailyEnergyExported: Double = 0.0,
        dailyEnergyImported: Double = 0.0,
        dailyEnergyConsumed: Double = 0.0,
    ) {
        transaction {
            HousePowerKpiTable.insert {
                it[HousePowerKpiTable.dateCreated] = dateCreated
                it[HousePowerKpiTable.solarPower] = solarPower
                it[HousePowerKpiTable.solarToHousePower] = solarToHousePower
                it[HousePowerKpiTable.gridToHousePower] = gridToHousePower
                it[HousePowerKpiTable.solarToGridPower] = solarToGridPower
                it[HousePowerKpiTable.ownConsumption] = ownConsumption
                it[HousePowerKpiTable.dailyEnergyYield] = dailyEnergyYield
                it[HousePowerKpiTable.dailyEnergySelfCovered] = dailyEnergySelfCovered
                it[HousePowerKpiTable.dailyEnergyExported] = dailyEnergyExported
                it[HousePowerKpiTable.dailyEnergyImported] = dailyEnergyImported
                it[HousePowerKpiTable.dailyEnergyConsumed] = dailyEnergyConsumed
            }
        }
    }

    fun seedRawKpiRow(
        dateCreated: Instant,
        meterExportedEnergy: Double = 0.0,
        meterImportedEnergy: Double = 0.0,
        inverterActivePower: Double = 0.0,
        meterActivePower: Double = 0.0,
        inverterDailyYield: Double = 0.0,
        inverterAccumulatedYield: Double = 0.0,
    ) {
        transaction {
            RawKpiRegisterValuesTable.insert {
                it[RawKpiRegisterValuesTable.dateCreated] = dateCreated
                it[RawKpiRegisterValuesTable.meterExportedEnergy] = meterExportedEnergy
                it[RawKpiRegisterValuesTable.meterImportedEnergy] = meterImportedEnergy
                it[RawKpiRegisterValuesTable.inverterActivePower] = inverterActivePower
                it[RawKpiRegisterValuesTable.meterActivePower] = meterActivePower
                it[RawKpiRegisterValuesTable.inverterDailyYield] = inverterDailyYield
                it[RawKpiRegisterValuesTable.inverterAccumulatedYield] = inverterAccumulatedYield
            }
        }
    }
}
