package ch.bziswiler.pvlogger.messaging.impl

import ch.bziswiler.pvlogger.messaging.SimpleMessageSender
import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.messaging.meta.MqConfig
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import mu.KotlinLogging
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.MessageProducer
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqMessageSender(override val queueName: QueueName, private val mqConfig: MqConfig) : SimpleMessageSender {

    private val logger = KotlinLogging.logger {}

    private val messageProducer: MessageProducer
    private val session: Session

    init {
        // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
        with(ActiveMQConnectionFactory(mqConfig.brokerUrl)) {
            userName = mqConfig.user
            password = mqConfig.passwd
            clientID = mqConfig.clientId
            with(createConnection()) {
                session = createSession(false, AUTO_ACKNOWLEDGE)
                val destination = session.createQueue(queueName.value)
                messageProducer = session.createProducer(destination)
                messageProducer.timeToLive = TWO.minutes()
                start()
            }
        }
    }

    override fun sendTagsAsMessage(tags: List<MessageTag>) {
        logger.debug { "Sending message ..." }
        val msg = session.createMessage()
        tags.forEach {
            msg.setStringProperty(it.key, it.value)
        }
        messageProducer.send(msg)
        logger.debug {
            """
SEND #########################################################################
${mqConfig.clientId}: sent message: $msg
SEND #########################################################################
"""
        }
    }
}

fun Long.minutes() = this.times(SECONDS_PER_MINUTE)
