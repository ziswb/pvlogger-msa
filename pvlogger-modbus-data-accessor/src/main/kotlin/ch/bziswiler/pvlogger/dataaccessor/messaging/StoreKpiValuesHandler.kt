package ch.bziswiler.pvlogger.dataaccessor.messaging

import ch.bziswiler.pvlogger.dataaccessor.database.access.HousePowerKpiDao
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import mu.KotlinLogging.logger

class StoreKpiValuesHandler {
    private val logger = logger {}

    fun store(data: PvLoggerKpiData) =
        data.let { kpis ->
            HousePowerKpiDao.store(kpis)
            logger.info { "KPIs stored: '$kpis'" }
        }
}
