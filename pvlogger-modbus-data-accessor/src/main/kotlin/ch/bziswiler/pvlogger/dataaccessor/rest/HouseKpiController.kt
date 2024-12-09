package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyConsumed
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyExported
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyImported
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dailyEnergyYield
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.dateCreated
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.gridToHousePower
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.ownConsumption
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.solarPower
import ch.bziswiler.pvlogger.dataaccessor.database.schema.HousePowerKpiTable.solarToGridPower
import ch.bziswiler.pvlogger.shared.rest.DataAccessorParameterNames.DATE
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_VERSION_PATH
import ch.bziswiler.pvlogger.shared.rest.KpiValuesDto
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import mu.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.ZoneId.of
import java.time.format.DateTimeFormatter.ofPattern

private const val UTC = "UTC"
private const val TWO_DECIMALS = "#.##"
private const val DATE_PATTERN = "yyyy-M-d"

fun Route.readHouseKpiValues() {
    route("/$DATA_ACCESSOR_READ_DATA_VERSION_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH") {
        get { call.respond(OK, HouseKpiController.todaysKpis()) }
        get("/{$DATE}") {
            call.parameters[DATE]?.let { date ->
                HouseKpiController.kpisOf(LocalDate.parse(date, ofPattern(DATE_PATTERN))).let { call.respond(OK, it) }
            } ?: call.respond(BadRequest)
        }
    }
}

object HouseKpiController {

    private val logger = KotlinLogging.logger {}
    private val rounder = DecimalFormat(TWO_DECIMALS)

    init {
        rounder.roundingMode = RoundingMode.HALF_UP
    }

    fun todaysKpis() = transaction {
        logger.info { "getting today's KPIs" }.let {
            HousePowerKpiTable
                .selectAll()
                .where { dateCreated.greaterEq(now().atStartOfDay()) }
                .orderBy(dateCreated)
                .map { toKpiValuesDto(it) }
                .toList()
        }
    }

    fun kpisOf(date: LocalDate) = transaction {
        logger.info { "getting KPIs of date '$date'" }.let {
            HousePowerKpiTable
                .selectAll()
                .where {
                    dateCreated.greaterEq(date.atStartOfDay())
                        .and(dateCreated.less(date.plusDays(1).atStartOfDay()))
                }
                .orderBy(dateCreated)
                .map { toKpiValuesDto(it) }
                .toList()
        }
    }

    fun getKpiById(id: Int): KpiValuesDto? =
        logger.info { "getting KPI by id '$id'" }.let {
            HousePowerKpiTable
                .selectAll()
                .where { HousePowerKpiTable.id.eq(id) }
                .map { toKpiValuesDto(it) }
                .firstOrNull()
        }

    private fun toKpiValuesDto(it: ResultRow) = KpiValuesDto(
        createdAt = it[dateCreated].atZone(of(UTC)).toInstant().toEpochMilli(),
        solarPower = rounder.format(it[solarPower]).toDouble(),
        gridPower = rounder.format(it[gridToHousePower].minus(it[solarToGridPower])).toDouble(),
        solarToHousePower = rounder.format(it[HousePowerKpiTable.solarToHousePower]).toDouble(),
        gridToHousePower = rounder.format(it[gridToHousePower]).toDouble(),
        solarToGridPower = rounder.format(it[solarToGridPower]).toDouble(),
        dailyEnergyYield = rounder.format(it[dailyEnergyYield]).toDouble(),
        ownConsumption = rounder.format(it[ownConsumption]).toDouble(),
        dailyEnergySelfCovered = rounder.format(it[HousePowerKpiTable.dailyEnergySelfCovered]).toDouble(),
        dailyEnergyExported = rounder.format(it[dailyEnergyExported]).toDouble(),
        dailyEnergyImported = rounder.format(it[dailyEnergyImported]).toDouble(),
        dailyEnergyConsumed = rounder.format(it[dailyEnergyConsumed]).toDouble()
    )
}
