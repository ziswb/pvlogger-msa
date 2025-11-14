package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.ModbusDao
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ReadDeviceIdentifiersResponse

class StoreModbusValuesHandler {
    fun store(data: ModbusResponse) =
        when (data) {
            is ModbusRegisterValueResponse -> ModbusDao.store(data)
            is ReadDeviceIdentifiersResponse -> ModbusDao.store(data)
        }
}
