package ch.bziswiler.pvlogger.dataaccessor.services.messaging

import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse

interface MqListener {

    fun messageReceived(message: ModbusResponse)
}