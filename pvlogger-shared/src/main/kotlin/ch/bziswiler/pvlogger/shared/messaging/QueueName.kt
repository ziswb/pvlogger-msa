package ch.bziswiler.pvlogger.shared.messaging

enum class QueueName(override val value: String) : Name {
    PERSIST_MODBUS("persist.modbus")
}