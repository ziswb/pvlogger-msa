package ch.bziswiler.pvlogger.messaging.meta

import java.io.Serializable

data class JobTag(val value: JobName, val key: String = "job") : Serializable {
    fun toSelector() = "$key = '${value.value}'"
}