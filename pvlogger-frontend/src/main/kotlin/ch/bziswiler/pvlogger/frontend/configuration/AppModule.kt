package ch.bziswiler.pvlogger.frontend.configuration

import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.KPI_EVENTS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.koin.dsl.module
import javax.jms.Topic

val appModule = module {
    single { AppConfig() }
    single<MqSessionFactory> { ActiveMqSessionFactory(get<AppConfig>().mqConfig) }
    single<MessageReceiver<Topic>> { ActiveMqTopicReceiver(get<MqSessionFactory>(), KPI_EVENTS) }
    single<MqReceiverComposer> {
        ActiveMqReceiverComposer(
            get<MqSessionFactory>(),
            get<MessageReceiver<Topic>>().destination,
            emptyList()
        )
    }
}
