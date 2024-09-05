package ch.bziswiler.pvlogger.heartbeat.messaging

import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName
import javax.jms.Message

interface MessageSender {

    val queueName: QueueName

    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())

    fun sendTaggedMessage(message: Message, tags: List<MessageTag> = emptyList())
}
