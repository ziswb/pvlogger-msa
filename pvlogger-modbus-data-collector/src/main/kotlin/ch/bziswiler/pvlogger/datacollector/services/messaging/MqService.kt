package ch.bziswiler.pvlogger.datacollector.services.messaging

import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.job.status.JobStatusMessage
import ch.bziswiler.pvlogger.shared.messaging.meta.MessageTag
import java.io.Serializable

interface MqService {

    fun addHandler(handler: MessageHandler)

    fun sendTaggedMessage(message: Serializable, tag: MessageTag)
}
