package ch.bziswiler.pvlogger.datacollector.services.messaging

import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusResponse

interface MqService {

    fun addListener(listener: MqListener)

    fun sendMessage(message: ModbusResponse)

    fun sendJobStatusMessage(message: JobStatusMessage)
}
