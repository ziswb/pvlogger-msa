package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.RawKpiRegisterValues
import ch.bziswiler.pvlogger.dataaccessor.database.access.RawKpiRegisterValuesDao
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_METER_ENERGY_TODAY
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES
import ch.bziswiler.pvlogger.dataaccessor.configuration.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Routing.rawKpiRegisterValues() {
    val appConfig by inject<AppConfig>()
    route("/$DATA_ACCESSOR_API_V1_PATH") {
        post("/$DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES") {
            val values = call.receive<RawKpiRegisterValues>()
            // Refused rather than ignored (ADR-0025): a backfill against a data
            // accessor without the dev setting would otherwise date a month of
            // raw rows "now" and today's energy would show the whole month.
            if (values.dateCreated != null && !appConfig.serverConfig.acceptClientTimestamps) {
                call.respond(HttpStatusCode.BadRequest, "dateCreated needs DEV_ACCEPT_CLIENT_TIMESTAMPS")
                return@post
            }
            RawKpiRegisterValuesDao.store(values, acceptClientTimestamp = appConfig.serverConfig.acceptClientTimestamps)
            call.respond(HttpStatusCode.OK)
        }
        get("/$DATA_ACCESSOR_READ_METER_ENERGY_TODAY") {
            val energy = RawKpiRegisterValuesDao.energyToday()
            call.respond(HttpStatusCode.OK, energy)
        }
    }
}
