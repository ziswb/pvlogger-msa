package ch.bziswiler.pvlogger.heartbeat.messaging

import ch.bziswiler.pvlogger.messaging.meta.JobTag

interface MqService {

    fun sendMessage(tags: List<JobTag> = emptyList())

    fun close()
}
