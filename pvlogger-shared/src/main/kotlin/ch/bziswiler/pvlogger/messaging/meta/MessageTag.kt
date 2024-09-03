package ch.bziswiler.pvlogger.messaging.meta

import java.io.Serializable

interface MessageTag : Serializable {
    val key: String
    val value: String
    fun toSelector() = "$key = '${value}'"
}
