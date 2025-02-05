package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName
import javax.jms.Queue

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqQueueSender(mqSessionFactory: MqSessionFactory, name: QueueName, ephemeralMessages: Boolean = false) : ActiveMqSenderScaffold(mqSessionFactory, name, ephemeralMessages) {

    override val destination: Queue
        get() = mqSessionFactory.session.createQueue(name.value)
}