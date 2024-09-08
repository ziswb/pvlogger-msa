package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName
import javax.jms.Queue
import javax.jms.Session

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqMessageReceiver(private val mqSessionFactory: MqSessionFactory, final override val queueName: QueueName) : MessageReceiver {

    private var session: Session = mqSessionFactory.session()
    private var queue: Queue = session.createQueue(queueName.value)

    override fun addMessageHandler(handler: MessageHandler) = with(session.createConsumer(queue, handler.messageSelector)) {
        messageListener = handler
    }
}
