package ch.bziswiler.pvlogger.shared.job.meta

import kotlinx.serialization.Serializable

@Serializable
data class JobId(
    val id: String,
)
