package ch.bziswiler.pvlogger.exporter.messaging

import ch.bziswiler.pvlogger.exporter.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.exporter.service.ExportService
import ch.bziswiler.pvlogger.shared.job.tags.ExportKpisMessageTag
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import javax.jms.Message

class KpiExportingMessageHandler(private val dataAccessor: DataAccessorRestProxy, private val exportService: ExportService) : MessageHandler {

    override val messageSelector = ExportKpisMessageTag().toSelector()

    private val logger = KotlinLogging.logger {}

    override fun onMessage(message: Message?) = when (message) {
        null -> logger.info { "message is null" }
        else -> exportKpis()
    }

    private fun exportKpis() {
        runBlocking {
            when (val data = dataAccessor.retrieveLatestKpis()) {
                null -> logger.warn { "KPIs to be exported is null!" }
                else -> exportService.export(data)
            }
        }
    }
}