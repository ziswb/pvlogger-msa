package ch.bziswiler.pvlogger.dataaccessor.services.messaging.impl

import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MessageHandler
import ch.bziswiler.pvlogger.dataaccessor.services.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.meta.MqConfig
import ch.bziswiler.pvlogger.shared.messaging.meta.QueueName
import org.apache.activemq.ActiveMQConnectionFactory
import javax.jms.Connection
import javax.jms.Queue
import javax.jms.Session
import javax.jms.Session.AUTO_ACKNOWLEDGE

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
class ActiveMqMessageReceiver(
    private val mqConfig: MqConfig,
    private val queueName: QueueName
) : MessageReceiver {

    private var session: Session
    private var queue: Queue

    init {
        with(connection()) {
            with(createSession(false, AUTO_ACKNOWLEDGE)) {
                session = this
                queue = createQueue(queueName.value)
            }
            start()
        }
    }

    override fun addMessageHandler(listener: MessageHandler) = with(session.createConsumer(queue, listener.messageSelector)) {
        messageListener = listener
    }

    private fun connection(): Connection {
        with(ActiveMQConnectionFactory(mqConfig.brokerUrl)) {
            userName = mqConfig.user
            password = mqConfig.passwd
            clientID = mqConfig.clientId
            return createConnection()
        }
    }
}
