package ch.bziswiler.pvlogger.kpi.services.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.meta.JobFinishedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.meta.JobName
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class KpiAggregatingMessageHandler : MessageHandler {

    override val messageSelector = JobFinishedMessageTag(JobName.DYNAMIC_DATA_JOB).toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) {
        logger.debug {
            """
RECEIVED #########################################################################
StoreModbusValuesMessageListener: message received: $message
RECEIVED #########################################################################
"""
        }
        if (message is ObjectMessage) {
            handleObjectMessage(message)
        } else {
            logger.info { "message is no object message! -> $message" }
        }
    }

    private fun handleObjectMessage(message: ObjectMessage) {
        logger.info { "message: $message" }
    }
}