package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto

interface ExportService {
    fun export(data: KpiValuesDto)
}