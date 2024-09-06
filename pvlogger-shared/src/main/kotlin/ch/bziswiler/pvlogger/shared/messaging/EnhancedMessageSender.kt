package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import javax.jms.Message

interface EnhancedMessageSender : SimpleMessageSender {

    fun sendTaggedMessage(message: Message, tags: List<MessageTag> = emptyList())
}
