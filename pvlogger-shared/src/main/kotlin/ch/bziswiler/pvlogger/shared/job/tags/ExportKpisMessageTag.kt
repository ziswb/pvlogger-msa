package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class ExportKpisMessageTag(
    override val key: String = "tag",
    override val value: String = "export.kpis"
) : MessageTag
