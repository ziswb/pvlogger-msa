package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.shared.job.tags.KpisStoredMessageTag
import ch.bziswiler.pvlogger.shared.job.tags.StoreKpisMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.messaging.TagsAsMessageSender
import mu.KotlinLogging
import org.jetbrains.exposed.dao.id.EntityID
import javax.jms.Message
import javax.jms.ObjectMessage

class StoreKpiValuesMessageHandler(private val messageSender: TagsAsMessageSender) : MessageHandler {

    override val messageSelector = StoreKpisMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        is ObjectMessage -> handleObjectMessage(message)
        else -> logger.info { "message is not an 'object' message! -> $message" }
    }

    private fun handleObjectMessage(message: ObjectMessage) = when (val obj = message.`object`) {
        is PvLoggerKpiData -> HousePowerKpiDao.storeAndGetId(obj).also { logger.trace { "KPI db-key = '$it'" } }.let { sendKpisStoredEvent(it) }
        else -> logger.info { "message.object is not 'pv logger kpi data'! -> $obj" }
    }

    private fun sendKpisStoredEvent(it: EntityID<Int>) = messageSender.sendTagsAsMessage(listOf(KpisStoredMessageTag(kpiId = it.value)))
}
