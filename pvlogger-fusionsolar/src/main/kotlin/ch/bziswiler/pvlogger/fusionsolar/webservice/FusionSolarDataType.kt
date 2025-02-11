package ch.bziswiler.pvlogger.fusionsolar.webservice

enum class FusionSolarDataType(val identifier: String) {
    STATION_REAL_KPI("STATION_REAL_KPI"),
    STATION_DAY_KPI("STATION_DAY_KPI"),
    STATION_MONTH_KPI("STATION_MONTH_KPI"),
    STATION_YEAR_KPI("STATION_YEAR_KPI"),
    DEVICE_REAL_KPI("DEVICE_REAL_KPI"),
    DEVICE_5_MIN_KPI("DEVICE_5_MIN_KPI")
}
