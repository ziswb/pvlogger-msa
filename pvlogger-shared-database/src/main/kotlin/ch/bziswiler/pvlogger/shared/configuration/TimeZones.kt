package ch.bziswiler.pvlogger.shared.configuration

import java.time.ZoneId

object TimeZones {
    val LOCAL_ZONE: ZoneId = ZoneId.systemDefault()

    fun requireConfigured() {
        require(LOCAL_ZONE.id != "UTC" && LOCAL_ZONE.id != "GMT") {
            "JVM timezone is '$LOCAL_ZONE' — set TZ environment variable (e.g. TZ=Europe/Zurich)"
        }
    }
}
