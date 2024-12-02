package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class KpisStoredMessageTag(
    override val key: String = "tag",
    override val value: String = "kpis.stored",
    val kpiId: Int
) : MessageTag
