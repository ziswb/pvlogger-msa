package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqListener
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.messaging.meta.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.messaging.meta.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.messaging.meta.StoreModbusRegisterValuesMessageTag
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.MessageListener
import javax.jms.MessageProducer
import javax.jms.ObjectMessage
import javax.jms.Queue
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
    private val persistEgressQueue: MessageProducer
    private val schedulerIngressQueue: Queue
    private val schedulerEgressQueue: MessageProducer

    init {
        val connection = connection()
        this.session = connection.createSession(false, AUTO_ACKNOWLEDGE)
        this.persistEgressQueue = setUpPersistEgressQueue()
        this.schedulerIngressQueue = session.createQueue(JOB_SCHEDULER.value)
        this.schedulerEgressQueue = setUpSchedulerEgressQueue()
        connection.start()
    }

    override fun addListener(listener: MqListener) {
        val messageConsumer = session.createConsumer(schedulerIngressQueue, listener.messageSelector)
        messageConsumer.messageListener = MessageListener { message ->
            logger.debug {
                """
RECEIVED #########################################################################
${listener.messageSelector}: message received: $message
RECEIVED #########################################################################
"""
            }
            listener.messageReceived()
        }
    }

    private fun setUpPersistEgressQueue(): MessageProducer {
        val egressQueue = session.createQueue(PERSIST_MODBUS.value)
        return session.createProducer(egressQueue)
    }

    private fun setUpSchedulerEgressQueue(): MessageProducer {
        val egressQueue = session.createQueue(JOB_SCHEDULER.value)
        return session.createProducer(egressQueue)
    }

    private fun connection(): Connection {
        val connectionFactory = ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)
        connectionFactory.userName = user
        connectionFactory.password = passwd
        connectionFactory.clientID = clientId
        return connectionFactory.createConnection()
    }

    override fun sendMessage(message: ModbusResponse) {
        logger.debug { "Sending message ..." }
        val taggedMessage = taggedMessage(message)
        persistEgressQueue.send(taggedMessage)
        logger.debug {
            """
SEND #########################################################################
$clientId: sent message with text='$taggedMessage'
SEND #########################################################################
"""
        }
    }

    private fun taggedMessage(message: ModbusResponse): ObjectMessage? =
        with(session.createObjectMessage(message)) {
            val msgTag = StoreModbusRegisterValuesMessageTag
            setStringProperty(msgTag.key(), msgTag.value())
            this
        }

    override fun sendJobStatusMessage(message: JobStatusMessage) {
        logger.debug { "Sending job finished message ..." }
        schedulerEgressQueue.send(session.createObjectMessage(message))
        logger.debug {
            """
SEND #########################################################################
$clientId: sent job finished message with job ID = '$message'
SEND #########################################################################
"""
        }
    }
}
