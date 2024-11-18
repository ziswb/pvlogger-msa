package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.database.connection.DatabaseFactory
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_MODBUS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MessageHandler> { StoreModbusValuesMessageHandler() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver> { ActiveMqMessageReceiver(get<MqSessionFactory>(), PERSIST_MODBUS) }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>()) }
}
