package ch.bziswiler.pvlogger.shared.messaging.meta

data class StoreModbusRegisterValuesMessageTag(
    override val key: String = "tag",
    override val value: String = "store.modbus.register-values"
) : MessageTag
