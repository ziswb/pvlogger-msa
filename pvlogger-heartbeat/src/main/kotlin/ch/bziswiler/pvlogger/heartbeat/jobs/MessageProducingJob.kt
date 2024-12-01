package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import org.quartz.Job

abstract class MessageProducingJob(private val messageSender: TagsAsMessageSender) : Job {
    fun sendTagAsMessage(tag: MessageTag) {
        messageSender.sendTagsAsMessage(listOf(tag))
    }
}