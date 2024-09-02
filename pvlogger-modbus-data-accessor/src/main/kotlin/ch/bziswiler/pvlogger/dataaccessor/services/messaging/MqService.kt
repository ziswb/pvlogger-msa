package ch.bziswiler.pvlogger.dataaccessor.services.messaging

import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.StoreModbusValuesMessageListener
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse

interface MqService {

    fun start(msgListener: StoreModbusValuesMessageListener)

    fun sendRegisterValuesSavedMessage(message: ModbusResponse)
}
