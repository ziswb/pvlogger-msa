package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.Name
import ch.bziswiler.pvlogger.shared.messaging.TaggedMessageSender
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import mu.KotlinLogging
import java.io.Serializable
import javax.jms.Destination
import javax.jms.Message
import javax.jms.MessageProducer

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

internal fun Long.minutes() = this.times(SECONDS_PER_MINUTE)

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
abstract class ActiveMqSenderScaffold(mqSessionFactory: MqSessionFactory, final override val name: Name, ephemeralMessages: Boolean = false) : TagsAsMessageSender, TaggedMessageSender {

    private val logger = KotlinLogging.logger {}

    private val messageProducer: MessageProducer
    internal val session = mqSessionFactory.session()

    internal abstract fun createDestination(): Destination

    init {
        // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
        messageProducer = session.createProducer(createDestination())
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