package ch.bziswiler.pvlogger.shared.messaging

import ch.bziswiler.pvlogger.shared.job.PvJob
import javax.jms.Message

class JobTriggeringMessageHandler(override val messageSelector: String, private val job: PvJob) : MessageHandler {
    override fun onMessage(message: Message) = job.execute()
}