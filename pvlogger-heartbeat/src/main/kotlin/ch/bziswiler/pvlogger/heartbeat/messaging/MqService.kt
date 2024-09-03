package ch.bziswiler.pvlogger.heartbeat.messaging

import ch.bziswiler.pvlogger.messaging.meta.JobStartMessageTag

interface MqService {

    fun sendMessage(tags: List<JobStartMessageTag> = emptyList())
}
