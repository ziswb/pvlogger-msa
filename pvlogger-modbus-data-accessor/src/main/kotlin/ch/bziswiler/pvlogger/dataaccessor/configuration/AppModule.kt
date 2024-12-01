package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.database.connection.DatabaseFactory
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageReceiver
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<StoreModbusValuesMessageHandler> { StoreModbusValuesMessageHandler() }
    single<StoreKpiValuesMessageHandler> { StoreKpiValuesMessageHandler() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<StoreModbusValuesMessageReceiver> { StoreModbusValuesMessageReceiver(get<MqSessionFactory>()) }
    single<StoreKpiValuesMessageReceiver> { StoreKpiValuesMessageReceiver(get<MqSessionFactory>()) }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>()) }
}
