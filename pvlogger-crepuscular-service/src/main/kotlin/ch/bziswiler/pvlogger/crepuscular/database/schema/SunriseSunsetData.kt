package ch.bziswiler.pvlogger.crepuscular.database.schema

import kotlinx.serialization.Serializable

@Serializable
data class SunriseSunsetData(val result: SunriseSunsetDataResult, val status: String)