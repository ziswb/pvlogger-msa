package ch.bziswiler.pvlogger.frontend.configuration

import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName.KPI_EVENTS
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.impl.ActiveMqTopicReceiver
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import javax.jms.Topic

val appModule = DI.Module(name = "appModule") {
    bind<AppConfig>() with singleton { AppConfig() }
    bind<MqSessionFactory>() with singleton { ActiveMqSessionFactory(instance<AppConfig>().mqConfig) }
    bind<MessageReceiver<Topic>>() with singleton { ActiveMqTopicReceiver(instance(), KPI_EVENTS) }
    bind<MqReceiverComposer>() with singleton {
        ActiveMqReceiverComposer(
            instance(),
            instance<MessageReceiver<Topic>>().destination,
            emptyList()
        )
    }
}
