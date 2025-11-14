package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.ModbusDao
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_DEVICE_ID_VALUES
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_MODBUS_VALUES
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post

fun Routing.storeModbusValues() {
    post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_MODBUS_VALUES") {
        val data = call.receive<ModbusRegisterValueResponse>()
        ModbusDao.store(data)
    }
    post("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_DEVICE_ID_VALUES") {
        val data = call.receive<ReadDeviceIdentifiersResponse>()
        ModbusDao.store(data)
    }
}
