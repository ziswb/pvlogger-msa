package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.dataaccessor.database.access.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_KPI_VALUES
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Routing.storeKpiValues() {
    route("/$DATA_ACCESSOR_API_V1_PATH") {
        post("/$DATA_ACCESSOR_STORE_KPI_VALUES") {
            val kpis = call.receive<PvLoggerKpiData>()
            HousePowerKpiDao.store(kpis)
            call.respond(HttpStatusCode.OK)
        }
    }
}
