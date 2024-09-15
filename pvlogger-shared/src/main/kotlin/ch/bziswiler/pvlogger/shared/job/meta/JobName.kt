package ch.bziswiler.pvlogger.shared.job.meta

import java.io.Serializable

enum class JobName(val value: String) : Serializable {
    DYNAMIC_DATA_JOB("modbus.dynamic.run"),
    STATIC_DATA_JOB("modbus.static.run"),
    DEVICE_IDS_JOB("modbus.devids.run"),
    DEVICE_LIST_JOB("modbus.devlist.run")
}