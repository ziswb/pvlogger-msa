package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.tags.MessageTag

interface TaggedMessageSender : NameAware {
    // TODO: clean up / was a mistake
    fun tagAndSendMessage(message: PvLoggerKpiData, tag: MessageTag)
}
