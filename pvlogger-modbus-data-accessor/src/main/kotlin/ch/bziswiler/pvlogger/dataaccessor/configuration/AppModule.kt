package ch.bziswiler.pvlogger.dataaccessor.configuration

import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreKpiValuesHandler
import ch.bziswiler.pvlogger.dataaccessor.messaging.StoreModbusValuesHandler
import ch.bziswiler.pvlogger.shared.databbase.DatabaseFactory
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module

val appModule =
    module {
        single<AppConfig> { AppConfig() }
        single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
        single<StoreModbusValuesHandler> { StoreModbusValuesHandler() }
        single<StoreKpiValuesHandler> { StoreKpiValuesHandler() }
        single<DatabaseFactory> { DatabaseFactory(get<AppConfig>().databaseConfig) }
    }
