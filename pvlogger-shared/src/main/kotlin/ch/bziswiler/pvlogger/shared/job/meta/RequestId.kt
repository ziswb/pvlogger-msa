package ch.bziswiler.pvlogger.shared.job.meta

import kotlinx.serialization.Serializable

@Serializable
data class RequestId(
    val id: UShort,
)
