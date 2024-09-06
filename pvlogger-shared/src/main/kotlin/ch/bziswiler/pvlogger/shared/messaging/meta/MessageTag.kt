package ch.bziswiler.pvlogger.shared.messaging.meta

import java.io.Serializable

interface MessageTag : Serializable {
    val key: String
    val value: String
    fun toSelector() = "$key = '${value}'"
}
