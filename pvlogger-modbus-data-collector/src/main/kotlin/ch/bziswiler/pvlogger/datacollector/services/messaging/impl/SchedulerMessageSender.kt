package ch.bziswiler.pvlogger.datacollector.services.messaging.impl

import ch.bziswiler.pvlogger.datacollector.services.messaging.MqService
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.shared.messaging.meta.MqConfig
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName.PERSIST_MODBUS
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import java.io.Serializable
import javax.jms.Connection
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
class MqServiceImpl(private val mqConfig: MqConfig) : MqService {

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

    override fun addHandler(handler: MessageHandler) {
        val messageConsumer = session.createConsumer(schedulerIngressQueue, handler.messageSelector)
        messageConsumer.messageListener = handler
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
        with(ActiveMQConnectionFactory(mqConfig.brokerUrl)) {
            userName = mqConfig.user
            password = mqConfig.passwd
            clientID = mqConfig.clientId
            return createConnection()
        }
    }

    override fun sendTaggedMessage(message: Serializable, tag: MessageTag) {
        logger.debug { "Sending message ..." }
        val taggedMessage = session.createObjectMessage(message)
        taggedMessage.setStringProperty(tag.key, tag.value)
        persistEgressQueue.send(taggedMessage)
        logger.debug {
            """
SEND #########################################################################
${mqConfig.clientId}: sent message with text='$taggedMessage'
SEND #########################################################################
"""
        }
    }
}

//        val tag = StoreModbusRegisterValuesMessageTag()
//        val tag = JobFinishedMessageTag(DYNAMIC_DATA_JOB)

fun Long.minutes() = this.times(SECONDS_PER_MINUTE)
