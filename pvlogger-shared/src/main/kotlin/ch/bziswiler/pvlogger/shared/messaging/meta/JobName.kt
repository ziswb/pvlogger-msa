package ch.bziswiler.pvlogger.shared.messaging.meta

enum class JobName(val value: String) {
    DYNAMIC_DATA_JOB("modbus.dynamic.run"),
    STATIC_DATA_JOB("modbus.static.run"),
    DEVICE_IDS_JOB("modbus.devids.run"),
    DEVICE_LIST_JOB("modbus.devlist.run")
}