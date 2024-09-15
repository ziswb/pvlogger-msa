package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.shared.messaging.TagsMessageSender
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import org.quartz.Job

abstract class MessageProducingJob(private val messageSender: TagsMessageSender) : Job {
    fun sendTagAsMessage(tag: MessageTag) {
        messageSender.sendTagsAsMessage(listOf(tag))
    }
}