package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.configuration.ExportConfig
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime.from
import java.time.LocalDateTime.now
import java.time.LocalDateTime.parse
import java.time.format.DateTimeFormatter.ofPattern

class JsonFileService(exportConfig: ExportConfig) : ExportService {

    private val logger = KotlinLogging.logger {}
    private val dateTimeFormatter = ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    private val path = exportConfig.path
    private val name = exportConfig.name
    private val myJson = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        encodeDefaults = true
    }

    override fun export(data: KpiValuesDto) {
        val createdAt = from(ofEpochMilli(data.createdAt)).format(dateTimeFormatter) ?: now().format(dateTimeFormatter)
        write(
            KpiExportRecord(
                createdAt,
                CurrentDataRecord(
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
    }

    private fun write(record: KpiExportRecord) {
        logger.info { "appending a record to the JSON file" }
        val file = File(path, "$name-${parse(record.lastUpdateTime, ofPattern("yyyy-MM-dd HH:mm:ss")).format(dateTimeFormatter)}.json")
        assertFileExists(file)
        exportRecordToJson(record, file)
    }

    private fun assertFileExists(file: File) {
        if (!file.exists()) {
            logger.info { "creating new file '$file'" }
            file.createNewFile()
        }
    }

    private fun exportRecordToJson(record: KpiExportRecord, file: File) = myJson.encodeToString(record).let { file.writeText(it) }
}
