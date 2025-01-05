package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData

interface ExportService {
    fun export(data: PvLoggerKpiData)
}