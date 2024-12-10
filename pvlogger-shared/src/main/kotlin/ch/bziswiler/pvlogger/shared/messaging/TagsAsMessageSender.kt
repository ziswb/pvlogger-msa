package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.tags.MessageTag

interface TagsAsMessageSender : NameAware {
    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())
}
