package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.heartbeat.messaging.MqService
import ch.bziswiler.pvlogger.messaging.meta.JobTag
import org.koin.java.KoinJavaComponent.inject
import org.quartz.Job

abstract class MessageProducingJob() : MqService, Job {

    private val mqService by inject<MqService>(MqService::class.java)

    override fun sendMessage(tags: List<JobTag>) = mqService.sendMessage(tags)

    override fun close() = mqService.close()
}
