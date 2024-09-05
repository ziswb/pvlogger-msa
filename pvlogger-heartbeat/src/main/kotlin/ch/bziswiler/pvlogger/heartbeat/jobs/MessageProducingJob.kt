package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.heartbeat.messaging.MessageSender
import ch.bziswiler.pvlogger.messaging.meta.MessageTag
import org.koin.java.KoinJavaComponent.inject
import org.quartz.Job
import javax.jms.Message

abstract class MessageProducingJob() : MessageSender, Job {

    private val messageSender by inject<MessageSender>(MessageSender::class.java)

    override fun sendTagsAsMessage(tags: List<MessageTag>) = messageSender.sendTagsAsMessage(tags)

    override fun sendTaggedMessage(message: Message, tags: List<MessageTag>) {
        throw UnsupportedOperationException("Not implemented yet")
    }
}
