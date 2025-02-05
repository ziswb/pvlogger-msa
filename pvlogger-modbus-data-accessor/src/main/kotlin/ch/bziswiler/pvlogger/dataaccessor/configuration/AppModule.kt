package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesMessageReceiver
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesMessageReceiver
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.core.module.dsl.named
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<StoreModbusValuesMessageHandler> { StoreModbusValuesMessageHandler() }
    single<StoreKpiValuesMessageHandler> { StoreKpiValuesMessageHandler() }
    single<StoreModbusValuesMessageReceiver> { StoreModbusValuesMessageReceiver(get<MqSessionFactory>()) }
    single<StoreKpiValuesMessageReceiver> { StoreKpiValuesMessageReceiver(get<MqSessionFactory>()) }
    single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<StoreKpiValuesMessageReceiver>().destination,
            listOf(get<StoreKpiValuesMessageHandler>())
        )
    } withOptions {
        named("kpi_values")
    }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<StoreModbusValuesMessageReceiver>().destination,
            listOf(get<StoreModbusValuesMessageHandler>())
        )
    } withOptions {
        named("modbus_values")
    }
}
