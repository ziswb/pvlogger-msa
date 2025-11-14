package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.PvLoggerKpiData
import ch.bziswiler.pvlogger.shared.messaging.QueueName
import ch.bziswiler.pvlogger.shared.tags.MessageTag
import javax.jms.Queue

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqQueueSender(mqSessionFactory: MqSessionFactory, name: QueueName, ephemeralMessages: Boolean = false) : ActiveMqSenderScaffold(mqSessionFactory, name, ephemeralMessages) {

    override val destination: Queue
        get() = mqSessionFactory.session.createQueue(name.value)

    // TODO: clean up / was just to make it compile
    override fun tagAndSendMessage(
        message: PvLoggerKpiData,
        tag: MessageTag
    ) {
        TODO("Not yet implemented")
    }
}