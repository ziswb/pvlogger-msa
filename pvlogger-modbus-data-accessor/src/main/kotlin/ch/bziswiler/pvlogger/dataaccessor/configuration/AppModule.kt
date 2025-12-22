package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    }
