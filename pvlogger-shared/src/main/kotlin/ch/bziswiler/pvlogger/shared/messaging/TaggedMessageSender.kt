package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import java.io.Serializable

interface TaggedMessageSender : QueueAware {
    fun tagAndSendMessage(message: Serializable, tag: MessageTag)
}
