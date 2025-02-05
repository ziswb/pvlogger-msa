package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.QueueName
import javax.jms.Queue

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqQueueReceiver(private val mqSessionFactory: MqSessionFactory, override val name: QueueName) : MessageReceiver<Queue> {

    override val destination: Queue
        get() = mqSessionFactory.session.createQueue(name.value)
}
