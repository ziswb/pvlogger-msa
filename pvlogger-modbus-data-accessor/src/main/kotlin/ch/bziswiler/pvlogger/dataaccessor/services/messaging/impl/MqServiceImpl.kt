package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqListener
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqService
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.ObjectMessage
import javax.jms.Session
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

    // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
    private val session: Session

    init {
        val connection = connection()
        this.session = connection.createSession(false, AUTO_ACKNOWLEDGE)
        connection.start()
    }

    override fun setListener(listener: MqListener) {
        val ingressQueue = session.createQueue(appConfig.mqConfig.ingressQueueName)
        val messageConsumer = session.createConsumer(ingressQueue)
        messageConsumer.messageListener = MessageListener { message: Message? ->
            if (message is ObjectMessage && message.`object` is ModbusResponse) {
                logger.debug {
                    """
RECEIVED #########################################################################
$clientId: message received()
RECEIVED #########################################################################
""""
                }
                listener.messageReceived(message.`object` as ModbusResponse)
            } else {
                // TODO log error!
                error("message is no object message! -> $message")
            }
        }
    }

    override fun sendRegisterValuesSavedMessage() {
        logger.warn { "TODO('Not yet implemented')" }
    }

    private fun connection(): Connection {
        val connectionFactory = ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)
        connectionFactory.userName = user
        connectionFactory.password = passwd
        connectionFactory.clientID = clientId
        return connectionFactory.createConnection()
    }
}
