package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.shared.job.tags.StoreKpisMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import mu.KotlinLogging
import javax.jms.Message
import javax.jms.ObjectMessage

class StoreKpiValuesMessageHandler : MessageHandler {

    override val messageSelector = StoreKpisMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is PvLoggerKpiData -> HousePowerKpiDao.storeAndGetId(obj).let { logger.trace { "KPI db-key = '$it'" } }
        else -> logger.info { "message.object is not 'pv logger kpi data'! -> $obj" }
    }
}