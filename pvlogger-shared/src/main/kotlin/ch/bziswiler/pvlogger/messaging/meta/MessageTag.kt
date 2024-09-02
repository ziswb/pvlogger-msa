package ch.bziswiler.pvlogger.messaging.meta

interface MessageTag {

    fun key() = "key"

    fun value(): String

    fun toSelector() = "${key()} = '${value()}'"
}