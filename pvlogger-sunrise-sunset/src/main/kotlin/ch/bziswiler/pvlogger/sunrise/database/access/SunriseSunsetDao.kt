package ch.bziswiler.pvlogger.sunrise.database.access

import ch.bziswiler.pvlogger.sunrise.database.schema.SolarTable
import ch.bziswiler.pvlogger.sunrise.database.schema.SunriseSunsetData
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class SunriseSunsetDao {

    private fun <T> dbQuery(block: () -> T): T = transaction { block() }

    fun save(data: SunriseSunsetData) = dbQuery {
        SolarTable.insert {
            it[status] = data.status
            it[time_zone_id] = data.tzid
            it[sunrise] = data.results.sunrise
            it[sunset] = data.results.sunset
            it[solar_noon] = data.results.solar_noon
            it[day_length] = data.results.day_length
            it[civil_twilight_begin] = data.results.civil_twilight_begin
            it[civil_twilight_end] = data.results.civil_twilight_end
            it[nautical_twilight_begin] = data.results.nautical_twilight_begin
            it[nautical_twilight_end] = data.results.nautical_twilight_end
            it[astronomical_twilight_begin] = data.results.astronomical_twilight_begin
            it[astronomical_twilight_end] = data.results.astronomical_twilight_end
        }
    }
}