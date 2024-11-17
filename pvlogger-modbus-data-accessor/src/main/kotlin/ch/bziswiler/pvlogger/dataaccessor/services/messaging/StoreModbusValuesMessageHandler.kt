package ch.bziswiler.pvlogger.dataaccessor.services.messaging

import ch.bziswiler.pvlogger.dataaccessor.services.persistence.ModbusDao
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.job.tags.StoreModbusRegisterValuesMessageTag
import ch.bziswiler.pvlogger.shared.modbus.tcp.response.ModbusRegisterValueResponse
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class StoreModbusValuesMessageHandler : MessageHandler {

    override val messageSelector = StoreModbusRegisterValuesMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) {
        logger.debug {
            """
RECEIVED #########################################################################
StoreModbusValuesMessageListener: message received: $message
RECEIVED #########################################################################
"""
        }
        if (message is ObjectMessage) {
            handleObjectMessage(message)
        } else {
            logger.info { "message is no object message! -> $message" }
        }
    }

    private fun handleObjectMessage(message: ObjectMessage) {
        val obj = message.`object`
        if (obj is ModbusRegisterValueResponse) {
            ModbusDao.store(obj)
        } else {
            logger.info { "message.object is no modbus register value response! -> $obj" }
        }
    }
}