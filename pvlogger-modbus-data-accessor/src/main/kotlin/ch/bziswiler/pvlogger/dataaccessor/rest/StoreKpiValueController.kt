package ch.bziswiler.pvlogger.dataaccessor.rest

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_API_V1_PATH
import ch.bziswiler.pvlogger.shared.rest.DataAccessorPaths.DATA_ACCESSOR_STORE_KPI_VALUES
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post

fun Routing.storeKpiValues() {
    post<PvLoggerKpiData>("/$DATA_ACCESSOR_API_V1_PATH/$DATA_ACCESSOR_STORE_KPI_VALUES") {
        val kpis = call.receive<PvLoggerKpiData>()
        HousePowerKpiDao.store(kpis)
    }
}
