package ch.bziswiler.pvlogger.crepuscular.configuration

import ch.bziswiler.pvlogger.crepuscular.database.access.SunriseSunsetDao
import ch.bziswiler.pvlogger.crepuscular.service.SunriseSunsetService
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<SunriseSunsetDao> { SunriseSunsetDao() }
    single<SunriseSunsetService> { SunriseSunsetService(get<AppConfig>().sunriseSunsetServiceConfig, get<SunriseSunsetDao>()) }
}
