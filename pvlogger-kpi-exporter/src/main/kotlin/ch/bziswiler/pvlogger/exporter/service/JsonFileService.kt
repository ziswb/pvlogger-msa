package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.configuration.ExportConfig
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File

class JsonFileService(exportConfig: ExportConfig, private val directoryService: DirectoryService) : ExportService {

    private val logger = KotlinLogging.logger {}
    private val lastUpdateTimeFormatter = Format { yearMonthDayPattern();char(' ');hour();char(':');minute();char(':');second() }
    private val fileNameDateFormatter = Format { yearMonthDayPattern();char('_');hour();char('-');minute();char('-');second();char('-');secondFraction(3) }
    private val path = exportConfig.path
    private val name = exportConfig.name
    private val root = exportConfig.root
    private val myJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
    }

    private fun DateTimeFormatBuilder.WithDateTime.yearMonthDayPattern() {
        year();char('-');monthNumber();char('-');dayOfMonth();
    }

    override fun export(data: KpiValuesDto) {
        val localDateTime = fromEpochMilliseconds(data.createdAt).toLocalDateTime(UTC)
        write(
            localDateTime, KpiExportRecord(
                lastUpdateTime = lastUpdateTimeFormatter.format(localDateTime), CurrentDataRecord(
                    pv_prod_akt_w = data.solarPower,
                    pv_verk_akt_w = data.solarToGridPower,
                    pv_kauf_akt_w = data.gridToHousePower,
                    pv_verb_akt_w = data.solarToHousePower + data.gridToHousePower,
                ), CurrentDayDataRecord(
                    pv_prod_tag_wh = data.dailyEnergyYield,
                    pv_verk_tag_wh = data.dailyEnergyExported,
                    pv_kauf_tag_wh = data.dailyEnergyImported,
                    pv_verb_tag_wh = data.dailyEnergyConsumed,
                )
            )
        )
        directoryService.cleanUpExportDirectory()
    }

    private fun write(createdAt: LocalDateTime, record: KpiExportRecord) {
        logger.info { "writing KPI record to JSON file" }
        val file = File(File(root, path), "$name-${createdAt.format(fileNameDateFormatter)}.json")
        assertFileExists(file)
        exportRecordToJson(record, file)
    }

    private fun assertFileExists(file: File) {
        val parentDirectory = file.parentFile
        if (!parentDirectory.exists()) {
            logger.info { "creating parent directory '$parentDirectory'" }
            parentDirectory.mkdirs()
        }
        if (!file.exists()) {
            logger.info { "creating new file '$file'" }
            file.createNewFile()
        }
    }

    private fun exportRecordToJson(record: KpiExportRecord, file: File) = myJson.encodeToString(record).let { file.writeText(it) }
}
