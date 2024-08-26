package ch.bziswiler.pvlogger.datacollector.services.messaging

import ch.bziswiler.pvlogger.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse

interface MqService {

    fun addListener(listener: MqListener)

    fun sendMessage(message: ModbusResponse)

    fun sendJobStatusMessage(message: JobStatusMessage)
}
