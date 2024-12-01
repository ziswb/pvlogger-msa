package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.tags.MessageTag

interface TagsAsMessageSender : QueueAware {
    fun sendTagsAsMessage(tags: List<MessageTag> = emptyList())
}
