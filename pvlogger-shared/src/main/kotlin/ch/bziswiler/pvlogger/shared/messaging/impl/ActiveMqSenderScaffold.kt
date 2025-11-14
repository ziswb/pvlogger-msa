package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.Name
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.messaging.TaggedMessageSender
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import mu.KotlinLogging
import java.io.Serializable
import javax.jms.Destination
import javax.jms.MessageProducer
import javax.jms.Session

private const val SECONDS_PER_MINUTE = 60
private const val TWO = 2L

internal fun Long.minutes() = this.times(SECONDS_PER_MINUTE)

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
abstract class ActiveMqSenderScaffold(internal val mqSessionFactory: MqSessionFactory, override val name: Name, private val ephemeralMessages: Boolean = false) : TagsAsMessageSender, TaggedMessageSender {

    private val logger = KotlinLogging.logger {}

    internal abstract val destination: Destination

    // see https://activemq.apache.org/components/classic/documentation/connection-configuration-uri for details
    private fun messageProducer(session: Session): MessageProducer {
        val mp = session.createProducer(destination)
        if (ephemeralMessages) {
            mp.timeToLive = TWO.minutes()
        }
        return mp
    }

    override fun sendTagsAsMessage(tags: List<MessageTag>) {
        logger.debug { "Sending tags as message ..." }
        val session = mqSessionFactory.session
        val msg = session.createMessage()
        tags.forEach { msg.setStringProperty(it.key, it.value) }
        messageProducer(session).send(msg)
        logger.debug {
            """
    SEND #########################################################################
    ActiveMqMessageSender: sent message with text='$msg'
    SEND #########################################################################
    """
        }
        session.close()
    }

    // TODO: clean up / was just to make it compile
    override fun tagAndSendMessage(message: PvLoggerKpiData, tag: MessageTag) {
        logger.debug { "Sending message ..." }
        val session = mqSessionFactory.session
        val taggedMessage = session.createObjectMessage(message as Serializable)
        taggedMessage.setStringProperty(tag.key, tag.value)
        messageProducer(session).send(taggedMessage)
        logger.debug {
            """
    SEND #########################################################################
    ActiveMqMessageSender: sent message with text='$taggedMessage'
    SEND #########################################################################
    """
        }
        session.close()
    }
}