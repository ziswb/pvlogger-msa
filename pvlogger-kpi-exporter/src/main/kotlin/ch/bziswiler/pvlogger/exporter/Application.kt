package ch.bziswiler.pvlogger.exporter

import ch.bziswiler.pvlogger.exporter.configuration.AppConfig
import ch.bziswiler.pvlogger.exporter.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.exporter.service.DirectoryService
import ch.bziswiler.pvlogger.exporter.service.JsonFileService
import ch.bziswiler.pvlogger.exporter.service.KpiExporter
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    startServices()
}

private fun startServices() =
    logger
        .info { "Starting exporter" }
        .also {
            KpiExporter(
                DataAccessorRestProxy(AppConfig.dataAccessConfig),
                JsonFileService(
                    AppConfig.exportConfig,
                    DirectoryService(AppConfig.exportConfig),
                ),
            ).exportKpis()
        }.also {
            logger.info { "Finished exporting" }
        }
