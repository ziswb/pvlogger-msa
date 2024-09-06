package ch.bziswiler.pvlogger.dataaccessor.di

import ch.bziswiler.pvlogger.dataaccessor.config.AppConfig
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactory
import ch.bziswiler.pvlogger.dataaccessor.database.DatabaseFactoryImpl
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl.StoreModbusValuesMessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName.PERSIST_MODBUS
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MessageHandler> { StoreModbusValuesMessageHandler() }
    single<MessageReceiver> { ActiveMqMessageReceiver(get<AppConfig>().mqConfig, PERSIST_MODBUS) }
    single<DatabaseFactory> { DatabaseFactoryImpl(get<AppConfig>()) }
}
