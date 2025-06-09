package ch.bziswiler.pvlogger.dataaccessor.database.access

import ch.bziswiler.pvlogger.dataaccessor.database.schema.CharacteristicCurveValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.UShortValueTable
import ch.bziswiler.pvlogger.shared.masterdata.modbus.Type
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusObject
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterIntValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterShortArrayValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterShortValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterStringValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterUIntValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterUShortValue
import ch.bziswiler.pvlogger.modbus.tcp.api.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.api.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.api.tcp.response.ModbusResponse
import ch.bziswiler.pvlogger.modbus.tcp.api.tcp.response.ReadDeviceIdentifiersResponse
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

object ModbusDao {

    private val logger = KotlinLogging.logger {}

    fun store(data: ModbusRegisterValueResponse) = transaction { storeValues(data, storeHeader(data)) }
    fun store(data: ReadDeviceIdentifiersResponse) = transaction { storeValues(data, storeHeader(data)) }

    private fun storeValues(data: ModbusRegisterValueResponse, headerId: EntityID<Int>) =
        data.registerValues.forEach { registerValue: ModbusRegisterValue<*> ->
            when (registerValue) {
                is ModbusRegisterUShortValue -> {
                    logger.trace { "Storing U16 value '${data}" }
                    storeUShortValues(registerValue, data, headerId)
                }

                is ModbusRegisterShortValue -> {
                    logger.trace { "Storing I16 value '${data}" }
                    ShortValueTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterUIntValue -> {
                    logger.trace { "Storing U32 value '${data}" }
                    storeUIntValues(registerValue, data, headerId)
                }

                is ModbusRegisterIntValue -> {
                    logger.trace { "Storing I32 value '${data}" }
                    IntegerValueTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterStringValue -> {
                    logger.trace { "Storing STR value '${data}" }
                    StringValuesTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterShortArrayValue -> {
                    logger.trace { "Storing MLD value '${data}" }
                    CharacteristicCurveValuesTable.store(headerId.value, registerValue)
                }
            }
        }

    private fun storeUIntValues(
        registerValue: ModbusRegisterUIntValue, data: ModbusRegisterValueResponse, headerId: EntityID<Int>
    ) {
        if (registerValue.definition.type == Type.U32) {
            logger.trace { "Storing U32 value '${data}" }
            UIntegerValueTable.store(headerId.value, registerValue)
        } else if (registerValue.definition.type == Type.BITFIELD_32) {
            logger.trace { "Storing BITFIELD_32 value '${data}" }
            UIntegerValueTable.store(headerId.value, registerValue)
        }
    }

    private fun storeUShortValues(
        registerValue: ModbusRegisterUShortValue, data: ModbusRegisterValueResponse, headerId: EntityID<Int>
    ) {
        if (registerValue.definition.type == Type.U16) {
            logger.trace { "Storing U16 value '${data}" }
            UShortValueTable.store(headerId.value, registerValue)
        } else if (registerValue.definition.type == Type.BITFIELD_16) {
            logger.trace { "Storing BITFIELD_16 value '${data}" }
            UShortValueTable.store(headerId.value, registerValue)
        }
    }

    private fun storeValues(data: ReadDeviceIdentifiersResponse, headerId: EntityID<Int>) =
        data.objectList.forEach { modbusObject: ModbusObject ->
            if (modbusObject.objectId.toInt() == 21 || modbusObject.objectId.toInt() == 135) {
                logger.trace { "Storing integer value '${data}" }
                IntegerValueTable.store(headerId.value, modbusObject)
            } else {
                logger.trace { "Storing string value '${data}" }
                StringValuesTable.store(headerId.value, modbusObject)
            }
        }

    private fun storeHeader(data: ModbusResponse): EntityID<Int> {
        logger.trace { "Storing header '${data}" }
        return ResponseTable.insertAndGetId {
            it[job_id] = data.jobId.id
            it[request_time] = data.responseTime
            it[response_time] = data.responseTime
            it[request_id] = data.header.protocolIdentifier
            it[protocol_type] = data.header.protocolType
            it[data_length] = data.header.dataLength
            it[logic_device_id] = data.header.logicDeviceId
            it[function_code] = data.functionCode.value
            it[number_of_bytes] = data.numberOfBytes
        }
    }
}