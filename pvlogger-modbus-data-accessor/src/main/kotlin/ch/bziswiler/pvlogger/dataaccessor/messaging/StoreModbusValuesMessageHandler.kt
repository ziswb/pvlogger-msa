package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.ModbusDao
import ch.bziswiler.pvlogger.shared.job.tags.StoreModbusRegisterValuesMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class StoreModbusValuesMessageHandler : MessageHandler {

    override val messageSelector = StoreModbusRegisterValuesMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is ModbusRegisterValueResponse -> ModbusDao.store(obj)
        else -> logger.info { "message.object is not a 'modbus register value response'! -> $obj" }
    }
}