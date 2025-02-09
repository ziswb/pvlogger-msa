package ch.bziswiler.pvlogger.crepuscular.database.access

import ch.bziswiler.pvlogger.crepuscular.database.schema.SolarTable
import ch.bziswiler.pvlogger.crepuscular.database.schema.SunriseSunsetData
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class SunriseSunsetDao {

    private fun <T> dbQuery(block: () -> T): T = transaction { block() }

    fun save(data: SunriseSunsetData) = dbQuery {
        SolarTable.insert {
            it[status] = data.status
            it[sunrise] = data.result.sunrise
            it[sunset] = data.result.sunset
            it[solar_noon] = data.result.solar_noon
            it[day_length] = data.result.day_length
            it[civil_twilight_begin] = data.result.civil_twilight_begin
            it[civil_twilight_end] = data.result.civil_twilight_end
            it[nautical_twilight_begin] = data.result.nautical_twilight_begin
            it[nautical_twilight_end] = data.result.nautical_twilight_end
            it[astronomical_twilight_begin] = data.result.astronomical_twilight_begin
            it[astronomical_twilight_end] = data.result.astronomical_twilight_end
        }
    }
}