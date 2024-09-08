package ch.bziswiler.pvlogger.shared.messaging

interface MessageReceiver : QueueAware {
    fun addMessageHandler(handler: MessageHandler)
}
