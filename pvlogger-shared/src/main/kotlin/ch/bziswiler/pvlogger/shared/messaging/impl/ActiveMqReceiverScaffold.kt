package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MessageReceiver
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.Name
import javax.jms.Destination
import javax.jms.Session

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
abstract class ActiveMqReceiverScaffold(private val mqSessionFactory: MqSessionFactory, final override val name: Name) : MessageReceiver {

    internal var session: Session = mqSessionFactory.session()
    private var destination: Destination = createDestination()

    internal abstract fun createDestination(): Destination

    override fun addMessageHandler(handler: MessageHandler) = with(session.createConsumer(destination, handler.messageSelector)) {
        messageListener = handler
    }
}
