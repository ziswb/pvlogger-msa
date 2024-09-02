package ch.bziswiler.pvlogger.dataaccessor.di

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactory
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.ReceivingMqService
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.SendingMqService
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.ReceivingMqServiceImpl
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.SendingMqServiceImpl
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.StoreModbusValuesMessageListener
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<SendingMqService> {
        SendingMqServiceImpl(get<AppConfig>())
    }
    single<StoreModbusValuesMessageListener> { StoreModbusValuesMessageListener(get<SendingMqService>()) }
    single<ReceivingMqService> {
        ReceivingMqServiceImpl(get<AppConfig>(), get<StoreModbusValuesMessageListener>())
    }
    single<DatabaseFactory> { DatabaseFactoryImpl(get<AppConfig>()) }
}
