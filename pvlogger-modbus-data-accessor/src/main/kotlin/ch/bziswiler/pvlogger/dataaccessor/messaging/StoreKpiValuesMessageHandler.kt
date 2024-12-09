package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.shared.job.tags.KpisCreatedMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class StoreKpiValuesMessageHandler : MessageHandler {

    override val messageSelector = KpisCreatedMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is PvLoggerKpiData -> HousePowerKpiDao.store(obj).let { logger.info { "KPIs stored: '$obj'" } }
        else -> logger.info { "message.object is not 'pv logger kpi data'! -> '$obj'" }
    }
}
