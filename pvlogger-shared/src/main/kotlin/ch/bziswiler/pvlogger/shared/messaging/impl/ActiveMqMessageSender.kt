package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName
import ch.bziswiler.pvlogger.shared.messaging.TaggedMessageSender
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import mu.KotlinLogging
import java.io.Serializable
import javax.jms.Message
import javax.jms.MessageProducer

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqMessageSender(mqSessionFactory: MqSessionFactory, final override val queueName: QueueName, ephemeralMessages: Boolean = false) : TagsAsMessageSender, TaggedMessageSender {

    private val logger = KotlinLogging.logger {}

    private val messageProducer: MessageProducer
    private val session = mqSessionFactory.session()

    init {
        // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
        messageProducer = session.createProducer(session.createQueue(queueName.value))
        if (ephemeralMessages) {
            messageProducer.timeToLive = TWO.minutes()
        }
    }

    override fun sendTagsAsMessage(tags: List<MessageTag>) {
        logger.debug { "Sending tags as message ..." }
        val msg = session.createMessage()
        tags.forEach { msg.setStringProperty(it.key, it.value) }
        sendMessage(msg)
    }

    override fun tagAndSendMessage(message: Serializable, tag: MessageTag) {
        logger.debug { "Sending message ..." }
        val taggedMessage = session.createObjectMessage(message)
        taggedMessage.setStringProperty(tag.key, tag.value)
        sendMessage(taggedMessage)
    }

    private fun sendMessage(taggedMessage: Message) = messageProducer.send(taggedMessage).also {
        logger.debug {
            """
    SEND #########################################################################
    ActiveMqMessageSender: sent message with text='$taggedMessage'
    SEND #########################################################################
    """
        }
    }
}

fun Long.minutes() = this.times(SECONDS_PER_MINUTE)
