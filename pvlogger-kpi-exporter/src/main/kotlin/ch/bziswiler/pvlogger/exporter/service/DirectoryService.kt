package ch.bziswiler.pvlogger.exporter.service

import ch.bziswiler.pvlogger.exporter.configuration.ExportConfig
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class DirectoryService(
    exportConfig: ExportConfig,
) {
    private val logger = KotlinLogging.logger {}
    private val path = exportConfig.path
    private val noOlderThanDateTime get() = LocalDateTime.now().minusHours(48).toInstant(UTC)

    fun cleanUpExportDirectory() {
        val noOlderThanDateTime = noOlderThanDateTime
        logger.info { "Inspecting files in folder $path for candidates to delete (older than '$noOlderThanDateTime')" }
        File(path)
            .listFiles()
            ?.filter { file ->
                Files
                    .readAttributes(file.toPath(), BasicFileAttributes::class.java)
                    .creationTime()
                    .toInstant()
                    .isBefore(noOlderThanDateTime)
            }?.sorted()
            ?.forEach { file ->
                logger.info { "Deleting $file" }
                file.delete()
            }
    }
}
