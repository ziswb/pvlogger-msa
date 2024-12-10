package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.tags.MessageTag
import java.io.Serializable

interface TaggedMessageSender : NameAware {
    fun tagAndSendMessage(message: Serializable, tag: MessageTag)
}
