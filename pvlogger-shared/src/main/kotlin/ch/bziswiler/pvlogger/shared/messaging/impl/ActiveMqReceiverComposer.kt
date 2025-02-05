package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MessageHandler
import ch.bziswiler.pvlogger.shared.messaging.MqReceiverComposer
import ch.bziswiler.pvlogger.shared.messaging.MqReconnectListener
import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import javax.jms.Destination

class ActiveMqReceiverComposer(
    private val factory: MqSessionFactory,
    private val destination: Destination,
    handlers: List<MessageHandler>,
) : MqReceiverComposer {

    private val noLocal = true
    private val msgHandlers = handlers.toMutableList()

    override fun compose() {
        factory.reconnectListener = object : MqReconnectListener {
            override fun onAfterReconnect() {
                registerHandlers()
            }
        }
        registerHandlers()
    }

    override fun addMessageHandler(handler: MessageHandler) {
        msgHandlers.add(handler)
    }

    private fun registerHandlers() {
        with(factory.session) {
            msgHandlers.forEach { handler ->
                createConsumer(destination, handler.messageSelector, noLocal).messageListener = handler
            }
        }
    }
}