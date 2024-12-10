package ch.bziswiler.pvlogger.frontend.configuration

import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.KPI_EVENTS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module

val appModule = module {
    single { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver> { ActiveMqTopicReceiver(get<MqSessionFactory>(), KPI_EVENTS) }
}
