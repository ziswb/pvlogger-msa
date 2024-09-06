package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName

interface SimpleMessageSender {

    val queueName: QueueName

    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())
}
