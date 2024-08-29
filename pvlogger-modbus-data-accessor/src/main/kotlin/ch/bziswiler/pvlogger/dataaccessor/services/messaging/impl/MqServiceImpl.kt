package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqService
import ch.bziswiler.pvlogger.dataaccessor.services.persistence.ModbusDao
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusRegisterValueResponse
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.ObjectMessage
import javax.jms.Session.AUTO_ACKNOWLEDGE

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class MqServiceImpl(private val appConfig: AppConfig) : MqService {

    private val logger = KotlinLogging.logger {}

    private val clientId = appConfig.mqConfig.clientId
    private val user = appConfig.mqConfig.user
    private val passwd = appConfig.mqConfig.passwd
    private val msgListener = MessageListener { message: Message? ->
        logger.debug {
            """
RECEIVED #########################################################################
$clientId: message received: $message
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
            sendRegisterValuesSavedMessage(obj)
        } else {
            logger.info { "message.object is no modbus register value response! -> $obj" }
        }
    }

    override fun start() {
        with(connection()) {
            with(createSession(false, AUTO_ACKNOWLEDGE)) {
                with(createConsumer(createQueue(QueueName.PERSIST_MODBUS.value))) { messageListener = msgListener }
            }
            start()
        }
    }

    override fun sendRegisterValuesSavedMessage(message: ModbusResponse) {
        logger.warn { "TODO('Not yet implemented') -> jobId: ${message.jobId.id}, requestId: ${message.requestId}" }
    }

    private fun connection(): Connection {
        val connectionFactory = ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)
        connectionFactory.userName = user
        connectionFactory.password = passwd
        connectionFactory.clientID = clientId
        return connectionFactory.createConnection()
    }
}
