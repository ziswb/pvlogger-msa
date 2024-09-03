package ch.bziswiler.pvlogger.heartbeat.jobs

import ch.bziswiler.pvlogger.heartbeat.messaging.MqService
import ch.bziswiler.pvlogger.messaging.meta.JobStartMessageTag
import org.koin.java.KoinJavaComponent.inject
import org.quartz.Job

abstract class MessageProducingJob() : MqService, Job {

    private val mqService by inject<MqService>(MqService::class.java)

    override fun sendMessage(tags: List<JobStartMessageTag>) = mqService.sendMessage(tags)
}
