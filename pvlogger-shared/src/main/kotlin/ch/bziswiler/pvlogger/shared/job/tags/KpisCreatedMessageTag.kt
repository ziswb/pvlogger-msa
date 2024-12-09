package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class KpisCreatedMessageTag(
    override val key: String = "tag",
    override val value: String = "kpis.created"
) : MessageTag
