package ch.bziswiler.pvlogger.shared.rest

import kotlinx.serialization.Serializable

@Serializable
data class Measurement<T>(val timestamp: Long, val value: T)
