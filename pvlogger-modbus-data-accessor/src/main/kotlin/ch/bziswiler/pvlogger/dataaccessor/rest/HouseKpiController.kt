package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.dataaccessor.rest.DataAccessorParameterNames.DATE
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_LATEST_KPI_PATH
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ofPattern

private const val DATE_PATTERN = "yyyy-M-d"

fun Route.readHouseKpiValues() {
    route("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_DATA_TODAY_VALUES_PATH") {
        get { call.respond(OK, HousePowerKpiDao.todaysKpis()) }
        get("/{$DATE}") {
            call.parameters[DATE]?.let { date ->
                HousePowerKpiDao.kpisOf(LocalDate.parse(date, ofPattern(DATE_PATTERN))).let { call.respond(OK, it) }
            } ?: call.respond(BadRequest)
        }
    }
    route("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_READ_LATEST_KPI_PATH") {
        get {
            when (val kpi = HousePowerKpiDao.latestKpi()) {
                null -> call.respond(NotFound)
                else -> call.respond(OK, kpi)
            }
        }
    }
}
