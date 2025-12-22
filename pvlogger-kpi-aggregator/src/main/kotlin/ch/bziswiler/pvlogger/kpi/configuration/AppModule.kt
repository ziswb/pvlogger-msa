package ch.bziswiler.pvlogger.kpi.configuration

import ch.bziswiler.pvlogger.kpi.rest.DataAccessorRestProxy
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<DataAccessorRestProxy> { DataAccessorRestProxy(get<AppConfig>().modbusDataAccessConfig) }
    }
