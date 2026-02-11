package ch.bziswiler.pvlogger.sunrise.configuration

import ch.bziswiler.pvlogger.sunrise.database.access.SunriseSunsetDao
import ch.bziswiler.pvlogger.sunrise.service.SunriseSunsetService
import ch.bziswiler.pvlogger.shared.database.DatabaseFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<SunriseSunsetDao> { SunriseSunsetDao() }
    single<SunriseSunsetService> { SunriseSunsetService(get<AppConfig>().sunriseSunsetServiceConfig, get<SunriseSunsetDao>()) }
}
