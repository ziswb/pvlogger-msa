package ch.bziswiler.pvlogger.kpi.configuration

import ch.bziswiler.pvlogger.kpi.messaging.KpiAggregatingMessageHandler
import ch.bziswiler.pvlogger.kpi.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.QueueName.PERSIST_KPIS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqMessageSender
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DataAccessorRestProxy> { DataAccessorRestProxy(get<AppConfig>().modbusDataAccessConfig) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<ActiveMqMessageSender> { ActiveMqMessageSender(get<MqSessionFactory>(), PERSIST_KPIS) }
    single<MessageHandler> { KpiAggregatingMessageHandler(get<DataAccessorRestProxy>(), get<ActiveMqMessageSender>()) }
    single<MessageReceiver> { ActiveMqMessageReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
}
