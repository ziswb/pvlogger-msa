package ch.bziswiler.pvlogger.dataaccessor.di

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactory
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MqService
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.MqServiceImpl
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqService> {
        MqServiceImpl(get<AppConfig>())
    }
    single<DatabaseFactory> { DatabaseFactoryImpl(get<AppConfig>()) }
}
