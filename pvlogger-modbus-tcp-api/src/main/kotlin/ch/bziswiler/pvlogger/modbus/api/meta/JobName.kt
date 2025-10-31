package ch.bziswiler.pvlogger.modbus.api.meta

enum class JobName(val value: String) {
    DYNAMIC_DATA_JOB("modbus.dynamic.run"),
    STATIC_DATA_JOB("modbus.static.run"),
    DEVICE_IDS_JOB("modbus.devids.run"),
    DEVICE_LIST_JOB("modbus.devlist.run"),
    DEVICE_5MIN_KPI_JOB("fusionsolar.dev5minkpi.run"),
    DEVICE_KPI_JOB("fusionsolar.devkpi.run"),
    EXTENDED_KPI_JOB("fusionsolar.extkpi.run"),
    STATION_KPI_JOB("fusionsolar.stnkpi.run"),
}