package ch.bziswiler.pvlogger.dataaccessor.services.persistence

import ch.bziswiler.pvlogger.dataaccessor.schema.CharacteristicCurveValuesTable
import ch.bziswiler.pvlogger.dataaccessor.schema.IntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.ResponseTable
import ch.bziswiler.pvlogger.dataaccessor.schema.ShortValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.StringValuesTable
import ch.bziswiler.pvlogger.dataaccessor.schema.UIntegerValueTable
import ch.bziswiler.pvlogger.dataaccessor.schema.UShortValueTable
import ch.bziswiler.pvlogger.masterdata.modbus.Type
import ch.bziswiler.pvlogger.modbus.ModbusObject
import ch.bziswiler.pvlogger.modbus.ModbusRegisterIntValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterShortArrayValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterShortValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterStringValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterUIntValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterUShortValue
import ch.bziswiler.pvlogger.modbus.ModbusRegisterValue
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ReadDeviceIdentifiersResponse
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

object ModbusDao {

    private val logger = KotlinLogging.logger {}

    fun store(data: ModbusRegisterValueResponse) = transaction { storeValues(data, storeHeader(data)) }
    fun store(data: ReadDeviceIdentifiersResponse) = transaction { storeValues(data, storeHeader(data)) }

    private fun storeValues(data: ModbusRegisterValueResponse, headerId: EntityID<Int>) =
        data.registerValues.forEach { registerValue: ModbusRegisterValue ->
            when (registerValue) {
                is ModbusRegisterUShortValue -> {
                    storeUShortValues(registerValue, data, headerId)
                }

                is ModbusRegisterShortValue -> {
                    logger.debug { "Storing I16 values '${data}" }
                    ShortValueTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterUIntValue -> {
                    storeUIntValues(registerValue, data, headerId)
                }

                is ModbusRegisterIntValue -> {
                    logger.debug { "Storing I32 values '${data}" }
                    IntegerValueTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterStringValue -> {
                    logger.debug { "Storing STR values '${data}" }
                    StringValuesTable.store(headerId.value, registerValue)
                }

                is ModbusRegisterShortArrayValue -> {
                    logger.debug { "Storing MLD values '${data}" }
                    CharacteristicCurveValuesTable.store(headerId.value, registerValue)
                }
            }
        }

    private fun storeUIntValues(
        registerValue: ModbusRegisterUIntValue, data: ModbusRegisterValueResponse, headerId: EntityID<Int>
    ) {
        if (registerValue.definition.type == Type.U32) {
            logger.debug { "Storing U32 values '${data}" }
            UIntegerValueTable.store(headerId.value, registerValue)
        } else if (registerValue.definition.type == Type.BITFIELD_32) {
            logger.debug { "Storing BITFIELD_32 values '${data}" }
            UIntegerValueTable.store(headerId.value, registerValue)
        }
    }

    private fun storeUShortValues(
        registerValue: ModbusRegisterUShortValue, data: ModbusRegisterValueResponse, headerId: EntityID<Int>
    ) {
        if (registerValue.definition.type == Type.U16) {
            logger.debug { "Storing U16 values '${data}" }
            UShortValueTable.store(headerId.value, registerValue)
        } else if (registerValue.definition.type == Type.BITFIELD_16) {
            logger.debug { "Storing BITFIELD_16 values '${data}" }
            UShortValueTable.store(headerId.value, registerValue)
        }
    }

    private fun storeValues(data: ReadDeviceIdentifiersResponse, headerId: EntityID<Int>) =
        data.objectList.forEach { modbusObject: ModbusObject ->
            if (modbusObject.objectId.toInt() == 21 || modbusObject.objectId.toInt() == 135) {
                logger.debug { "Storing integer values '${data}" }
                IntegerValueTable.store(headerId.value, modbusObject)
            } else {
                logger.debug { "Storing string values '${data}" }
                StringValuesTable.store(headerId.value, modbusObject)
            }
        }

    private fun storeHeader(data: ModbusResponse): EntityID<Int> {
        logger.debug { "Storing header '${data}" }
        return ResponseTable.insertAndGetId {
            it[job_id] = data.jobId
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