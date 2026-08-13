package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.RawKpiRegisterValues
import ch.bziswiler.pvlogger.dataaccessor.database.access.RawKpiRegisterValuesDao
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_READ_METER_ENERGY_TODAY
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Routing.rawKpiRegisterValues() {
    route("/$DATA_ACCESSOR_API_V1_PATH") {
        post("/$DATA_ACCESSOR_STORE_RAW_KPI_REGISTER_VALUES") {
            val values = call.receive<RawKpiRegisterValues>()
            RawKpiRegisterValuesDao.store(values)
            call.respond(HttpStatusCode.OK)
        }
        get("/$DATA_ACCESSOR_READ_METER_ENERGY_TODAY") {
            val energy = RawKpiRegisterValuesDao.energyToday()
            call.respond(HttpStatusCode.OK, energy)
        }
    }
}
