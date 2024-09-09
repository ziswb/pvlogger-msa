package ch.bziswiler.pvlogger.kpi.di

import ch.bziswiler.pvlogger.kpi.config.AppConfig
import ch.bziswiler.pvlogger.kpi.services.messaging.impl.KpiAggregatingMessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName.PERSIST_MODBUS
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<MessageHandler> { KpiAggregatingMessageHandler() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver> { ActiveMqMessageReceiver(get<MqSessionFactory>(), PERSIST_MODBUS) }
}
