package ch.bziswiler.pvlogger.kpi.configuration

import ch.bziswiler.pvlogger.kpi.messaging.KpiAggregatingMessageHandler
import ch.bziswiler.pvlogger.kpi.rest.DataAccessorRestProxy
import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.JOB_SCHEDULER
import ch.bziswiler.pvlogger.shared.messaging.TopicName.KPI_EVENTS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicSender
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single<DataAccessorRestProxy> { DataAccessorRestProxy(get<AppConfig>().modbusDataAccessConfig) }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<ActiveMqTopicSender> { ActiveMqTopicSender(get<MqSessionFactory>(), KPI_EVENTS) }
    single<MessageHandler> { KpiAggregatingMessageHandler(get<DataAccessorRestProxy>(), get<ActiveMqTopicSender>()) }
    single<MessageReceiver> { ActiveMqTopicReceiver(get<MqSessionFactory>(), JOB_SCHEDULER) }
}
