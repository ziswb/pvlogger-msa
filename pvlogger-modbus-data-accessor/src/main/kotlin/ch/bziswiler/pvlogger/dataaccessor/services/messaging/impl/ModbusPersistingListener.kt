package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqListener
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqService
import ch.bziswiler.pvlogger.dataaccessor.services.persistence.ModbusDao.store
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse

class ModbusPersistingListener(private val mqService: MqService) : MqListener {

    init {
        mqService.setListener(this)
    }

    override fun messageReceived(message: ModbusResponse) {
        when (message) {
            is ModbusRegisterValueResponse -> {
                store(message)
                mqService.sendRegisterValuesSavedMessage()
            }
        }
    }
}
