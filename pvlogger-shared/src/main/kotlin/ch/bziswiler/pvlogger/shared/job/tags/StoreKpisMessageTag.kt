package ch.bziswiler.pvlogger.shared.job.tags

import ch.bziswiler.pvlogger.shared.tags.MessageTag

data class StoreKpisMessageTag(
    override val key: String = "tag",
    override val value: String = "store.kpis"
) : MessageTag
