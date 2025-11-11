package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.configuration.ExportConfig
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import ch.bziswiler.pvlogger.shared.unit.KW
import ch.bziswiler.pvlogger.shared.unit.KWh
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
import java.nio.file.Files
import kotlin.io.path.isSymbolicLink

class JsonFileService(exportConfig: ExportConfig, private val directoryService: DirectoryService) : ExportService {

    private val logger = KotlinLogging.logger {}
    private val lastUpdateTimeFormatter = Format { yearMonthDayPattern();char(' ');hour();char(':');minute();char(':');second() }
    private val fileNameDateFormatter = Format { yearMonthDayPattern();char('_');hour();char('-');minute();char('-');second();char('-');secondFraction(3) }
    private val path = exportConfig.path
    private val name = exportConfig.name

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
        prettyPrint = true
    }

    private fun DateTimeFormatBuilder.WithDateTime.yearMonthDayPattern() {
        year();char('-');monthNumber();char('-');dayOfMonth();
    }

    override fun export(data: KpiValuesDto) {
        val localDateTime = fromEpochMilliseconds(data.createdAt).toLocalDateTime(UTC)
        write(
            localDateTime, KpiExportRecord(
                lastUpdateTime = lastUpdateTimeFormatter.format(localDateTime), CurrentDataRecord(
                    pv_prod_akt_w = KW(data.solarPower).toW().w,
                    pv_verk_akt_w = KW(data.solarToGridPower).toW().w,
                    pv_kauf_akt_w = KW(data.gridToHousePower).toW().w,
                    pv_verb_akt_w = KW((data.solarToHousePower + data.gridToHousePower)).toW().w,
                ), CurrentDayDataRecord(
                    pv_prod_tag_wh = KWh(data.dailyEnergyYield).toWh().wh,
                    pv_verk_tag_wh = KWh(data.dailyEnergyExported).toWh().wh,
                    pv_kauf_tag_wh = KWh(data.dailyEnergyImported).toWh().wh,
                    pv_verb_tag_wh = KWh(data.dailyEnergyConsumed).toWh().wh,
                )
            )
        )
        directoryService.cleanUpExportDirectory()
    }

    private fun write(createdAt: LocalDateTime, record: KpiExportRecord) {
        logger.info { "writing KPI record to JSON file" }
        val parent = File(path)
        val fileName = "$name-${createdAt.format(fileNameDateFormatter)}.json"
        val file = assertFileExists(parent, fileName)
        exportRecordToJson(record, file)
    }

    private fun assertFileExists(parent: File, fileName: String): File {
        var file = File(parent, fileName)
        if (parent.toPath().isSymbolicLink()) {
            logger.info { "parent directory '${parent.toPath()}' exists and is a symbolic link" }
            file = File(Files.readSymbolicLink(parent.toPath()).toFile(), fileName)
        } else if (parent.exists().not()) {
            logger.info { "creating parent directory '${parent}'" }
            parent.mkdirs()
        }
        if (!file.exists()) {
            logger.info { "creating new file '$file'" }
            file.createNewFile()
        }
        return file
    }

    private fun exportRecordToJson(record: KpiExportRecord, file: File) = json.encodeToString(record).let { file.writeText(it) }
}
