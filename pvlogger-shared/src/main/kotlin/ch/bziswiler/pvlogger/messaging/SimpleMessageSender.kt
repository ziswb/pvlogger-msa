package ch.bziswiler.pvlogger.messaging

import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.messaging.meta.QueueName

interface SimpleMessageSender {

    val queueName: QueueName

    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())
}
