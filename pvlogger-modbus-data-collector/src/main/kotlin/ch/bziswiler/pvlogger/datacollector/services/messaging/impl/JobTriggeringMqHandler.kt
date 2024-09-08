package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.services.modbus.ModbusDataCollectionJob
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import javax.jms.Message

class JobTriggeringMqHandler(override val messageSelector: String, private val job: ModbusDataCollectionJob) : MessageHandler {
    override fun onMessage(message: Message) = job.execute()
}