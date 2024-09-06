package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.shared.messaging.SimpleMessageSender
import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import org.quartz.Job

abstract class MessageProducingJob(private val messageSender: SimpleMessageSender) : Job {
    fun sendTagAsMessage(tag: MessageTag) {
        messageSender.sendTagsAsMessage(listOf(tag))
    }
}