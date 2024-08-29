package ch.bziswiler.pvlogger.dataaccessor.services.messaging

import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse

interface MqService {

    fun start()

    fun sendRegisterValuesSavedMessage(message: ModbusResponse)
}
