package ch.bziswiler.pvlogger.sunrise.database.schema

import org.jetbrains.exposed.dao.id.IntIdTable

object SolarTable : IntIdTable("solar_table") {
    val status = text("status")
    val time_zone_id = text("time_zone_id")
    val sunrise = text("sunrise")
    val sunset = text("sunset")
    val solar_noon = text("solar_noon")
    val day_length = integer("day_length")
    val civil_twilight_begin = text("civil_twilight_begin")
    val civil_twilight_end = text("civil_twilight_end")
    val nautical_twilight_begin = text("nautical_twilight_begin")
    val nautical_twilight_end = text("nautical_twilight_end")
    val astronomical_twilight_begin = text("astronomical_twilight_begin")
    val astronomical_twilight_end = text("astronomical_twilight_end")
}
