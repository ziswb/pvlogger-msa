package ch.bziswiler.pvlogger.messaging

import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import javax.jms.Message

interface EnhancedMessageSender {

    val queueName: QueueName

    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())

    fun sendTaggedMessage(message: Message, tags: List<MessageTag> = emptyList())
}
