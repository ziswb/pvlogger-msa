package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.config.AppConfig
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqListener
import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.messaging.meta.JobFinishedMessageTag
import ch.bziswiler.pvlogger.messaging.meta.JobName.DYNAMIC_DATA_JOB
import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.messaging.meta.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.messaging.meta.StoreModbusRegisterValuesMessageTag
import ch.bziswiler.pvlogger.modbus.tcp.response.ModbusResponse
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.MessageProducer
import javax.jms.Queue
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class MqServiceImpl(private val appConfig: AppConfig) : MqService {

    private val logger = KotlinLogging.logger {}

    // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
    private val session: Session
    private val persistEgressQueue: MessageProducer
    private val schedulerIngressQueue: Queue
    private val schedulerEgressQueue: MessageProducer

    init {
        with(connection()) {
            session = createSession(false, AUTO_ACKNOWLEDGE)
            with(session) {
                persistEgressQueue = setUpPersistEgressQueue()
                schedulerIngressQueue = createQueue(JOB_SCHEDULER.value)
                schedulerEgressQueue = setUpSchedulerEgressQueue()
            }
            start()
        }
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
        val producer = session.createProducer(egressQueue)
        producer.timeToLive = TWO.minutes()
        return producer
    }

    private fun connection(): Connection {
        with(ActiveMQConnectionFactory(appConfig.mqConfig.brokerUrl)) {
            userName = appConfig.mqConfig.user
            password = appConfig.mqConfig.passwd
            clientID = appConfig.mqConfig.clientId
            return createConnection()
        }
    }

    override fun sendMessage(message: ModbusResponse) {
        logger.debug { "Sending message ..." }
        val taggedMessage = message.toTaggedMessage(session, StoreModbusRegisterValuesMessageTag())
        persistEgressQueue.send(taggedMessage)
        logger.debug {
            """
SEND #########################################################################
${appConfig.mqConfig.clientId}: sent message with text='$taggedMessage'
SEND #########################################################################
"""
        }
    }


    override fun sendJobStatusMessage(message: JobStatusMessage) {
        logger.debug { "Sending job finished message ..." }
        val taggedMessage = message.toTaggedMessage(session, JobFinishedMessageTag(DYNAMIC_DATA_JOB))
        schedulerEgressQueue.send(taggedMessage)
        logger.debug {
            """
SEND #########################################################################
${appConfig.mqConfig.clientId}: sent job finished message with job ID = '$taggedMessage'
SEND #########################################################################
"""
        }
    }
}

fun ModbusResponse.toTaggedMessage(session: Session, msgTag: MessageTag): Message =
    with(session.createObjectMessage(this)) {
        setStringProperty(msgTag.key, msgTag.value)
        this
    }


fun JobStatusMessage.toTaggedMessage(session: Session, msgTag: JobFinishedMessageTag): Message =
    with(session.createObjectMessage(this)) {
        setStringProperty(msgTag.key, msgTag.value)
        this
    }

fun Long.minutes() = this.times(SECONDS_PER_MINUTE)
