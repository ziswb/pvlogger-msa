package ch.bziswiler.pvlogger.messaging.meta

data class StoreModbusRegisterValuesMessageTag(
    override val key: String = "tag",
    override val value: String = "store.modbus.register-values"
) : MessageTag
